package me.soknight.easydesk.channel.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.requests.send.media.SendMediaGroupData
import dev.inmo.tgbotapi.requests.send.media.SendPhotoData
import dev.inmo.tgbotapi.requests.send.media.SendStickerByFileId
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.toChatId
import io.ktor.http.ContentType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.telegram.domain.TelegramConversation

class TelegramConversationSendTest {

    private val bot = mockk<TelegramBot>(relaxed = true)
    private val channel = mockk<Channel>(relaxed = true)
    private val userChatId: ChatIdentifier = 12345L.toChatId()

    private val conversation = TelegramConversation(
        attributes = emptyMap(),
        bot = bot,
        channel = channel,
        userChatId = userChatId,
    )

    private val sentMessageId = MessageId(42L)

    @BeforeTest
    fun setUp() {
        clearMocks(bot)
        val sentMessage = mockk<ContentMessage<*>>(relaxed = true)
        every { sentMessage.messageId } returns sentMessageId
        coEvery { bot.execute(any<Request<ContentMessage<*>>>()) } returns sentMessage
    }

    @Test
    fun `text only message sends SendTextMessage request`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        conversation.send(makeMessage(text = "Hello"))

        val request = requestSlot.captured
        assertIs<SendTextMessage>(request)
        assertEquals("Hello", request.text)
    }

    @Test
    fun `single photo with telegram file_id sends request containing file_id`() = runTest {
        val photoMessage = makeMessage(text = "caption", attachments = listOf(makePhotoAttachment("photo_file_id_123")))
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val result = conversation.send(photoMessage)

        assertIs<SendPhotoData>(requestSlot.captured)
        assertEquals("42", result.nativeId)
    }

    @Test
    fun `sticker with telegram file_id sends SendStickerByFileId request`() = runTest {
        val stickerMessage = makeMessage(attachments = listOf(makeStickerAttachment("sticker_file_id_999")))
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val result = conversation.send(stickerMessage)

        assertIs<SendStickerByFileId>(requestSlot.captured)
        assertEquals("42", result.nativeId)
    }

    @Test
    fun `sticker without telegram file_id is skipped silently`() = runTest {
        val stickerMessage = makeMessage(attachments = listOf(makeStickerAttachmentNoFileId()))

        val result = conversation.send(stickerMessage)

        coVerify(exactly = 0) { bot.execute(any()) }
        assertEquals("0", result.nativeId)
    }

    @Test
    fun `two photo attachments use sendMediaGroup and return first chunk message id`() = runTest {
        val photoMessage = makeMessage(
            text = "caption text",
            attachments = listOf(
                makePhotoAttachment("file_id_1"),
                makePhotoAttachment("file_id_2"),
            ),
        )
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val result = conversation.send(photoMessage)

        assertIs<SendMediaGroupData>(requestSlot.captured)
        assertEquals("42", result.nativeId)
    }

    // -------------- HELPERS ------------------------------------------------------------------------------------------

    private fun makeMessage(
        text: String? = null,
        attachments: List<Attachment> = emptyList(),
    ): Message = object : Message {
        override val conversation: Conversation = this@TelegramConversationSendTest.conversation
        override val nativeId: String = ""
        override val sender: ChannelActor = ChannelActor.System
        override val receiver: ChannelActor = ChannelActor.Unknown
        override val plainText: String? = text
        override val attachments = attachments
        override val attributes: Attributes = emptyMap()
        override fun copy(block: MessageBuilder.() -> Unit): MessageBuilder = throw UnsupportedOperationException()
        override suspend fun delete(): Unit = throw UnsupportedOperationException()
        override suspend fun edit(block: MessageBuilder.() -> Unit): Message = throw UnsupportedOperationException()
        override suspend fun reply(block: MessageBuilder.() -> Unit): Message = throw UnsupportedOperationException()
    }

    private fun makePhotoAttachment(fileId: String): Attachment = object : Attachment.Photo {
        override val attributes: Attributes = mapOf("telegram.file_id" to JsonPrimitive(fileId))
        override val channel: Channel = this@TelegramConversationSendTest.channel
        override val contentType = ContentType.Image.JPEG
        override val fileName = "photo.jpg"
        override val fileSize: Long? = null
        override val height: Int = 100
        override val width: Int = 100
        override val contentSource get() = Buffer()
    }

    private fun makeStickerAttachment(fileId: String): Attachment = object : Attachment.Sticker {
        override val attributes: Attributes = mapOf("telegram.file_id" to JsonPrimitive(fileId))
        override val channel: Channel = this@TelegramConversationSendTest.channel
        override val contentType = ContentType.Image.Any
        override val fileName = "sticker.webp"
        override val fileSize: Long? = null
        override val height: Int = 512
        override val width: Int = 512
        override val contentSource get() = Buffer()
    }

    private fun makeStickerAttachmentNoFileId(): Attachment = object : Attachment.Sticker {
        override val attributes: Attributes = emptyMap()
        override val channel: Channel = this@TelegramConversationSendTest.channel
        override val contentType = ContentType.Image.Any
        override val fileName = "sticker.webp"
        override val fileSize: Long? = null
        override val height: Int = 512
        override val width: Int = 512
        override val contentSource get() = Buffer()
    }

}
