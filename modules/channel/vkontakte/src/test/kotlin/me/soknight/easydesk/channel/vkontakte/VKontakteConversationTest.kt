package me.soknight.easydesk.channel.vkontakte

import io.ktor.http.ContentType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.vkontakte.config.VKontakteConfig
import me.soknight.easydesk.channel.vkontakte.vk.VkBot
import me.soknight.easydesk.channel.vkontakte.vk.api.VkApiClient
import me.soknight.easydesk.channel.vkontakte.vk.api.VkDocUploadResponse
import me.soknight.easydesk.channel.vkontakte.vk.api.VkDocUploadServerResponse
import me.soknight.easydesk.channel.vkontakte.vk.api.VkSavedDoc
import me.soknight.easydesk.channel.vkontakte.vk.api.VkSavedDocResponse

class VKontakteConversationTest {

    private val apiClient = mockk<VkApiClient>()
    private val bot = VkBot(groupId = 1L, apiClient = apiClient)
    private val channel = VKontakteChannel(
        identifier = "test",
        config = VKontakteConfig(groupId = 1L, token = "token"),
    )
    private val conversation = VKontakteConversation(
        bot = bot,
        channel = channel,
        peerId = 100500L,
    )

    @Test
    fun `should_sendMessageWithDocAttachmentString_when_documentAttachmentUploaded`() = runTest {
        val docBytes = byteArrayOf(1, 2, 3, 4)
        val docAttachment = object : Attachment.Document {
            override val attributes: Attributes = emptyMap()
            override val channel get() = this@VKontakteConversationTest.channel
            override val contentSource: Source get() = Buffer().also { it.write(docBytes) }
            override val contentType: ContentType = ContentType.Application.OctetStream
            override val fileName: String = "report.pdf"
            override val fileSize: Long = docBytes.size.toLong()
        }

        val message = mockk<Message> {
            coEvery { attachments } returns listOf(docAttachment)
            coEvery { plainText } returns "Here is the document"
        }

        coEvery {
            apiClient.getDocUploadServer(peerId = 100500L, type = "doc")
        } returns VkDocUploadServerResponse(uploadUrl = "https://upload.vk.com/doc")

        coEvery {
            apiClient.uploadDocBytes(
                uploadUrl = "https://upload.vk.com/doc",
                bytes = docBytes,
                fileName = "report.pdf",
            )
        } returns VkDocUploadResponse(file = "file_token_abc")

        coEvery {
            apiClient.saveDoc(file = "file_token_abc")
        } returns VkSavedDocResponse(type = "doc", doc = VkSavedDoc(id = 1L, ownerId = 123L))

        coEvery {
            apiClient.sendMessage(
                peerId = 100500L,
                text = "Here is the document",
                attachments = listOf("doc123_1"),
                replyTo = null,
            )
        } returns 42

        val result = conversation.send(message, replyToNativeId = null)

        assertEquals("42", result.nativeId)

        coVerify(exactly = 1) { apiClient.getDocUploadServer(peerId = 100500L, type = "doc") }
        coVerify(exactly = 1) {
            apiClient.uploadDocBytes(
                uploadUrl = "https://upload.vk.com/doc",
                bytes = docBytes,
                fileName = "report.pdf",
            )
        }
        coVerify(exactly = 1) { apiClient.saveDoc(file = "file_token_abc") }
        coVerify(exactly = 1) {
            apiClient.sendMessage(
                peerId = 100500L,
                text = "Here is the document",
                attachments = listOf("doc123_1"),
                replyTo = null,
            )
        }
    }

}
