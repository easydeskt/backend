package me.soknight.easydesk.channel.email

import io.mockk.every
import io.mockk.mockk
import jakarta.mail.Multipart
import kotlinx.io.Buffer
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.email.domain.EmailConversation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmailConversationTest {

    private val channel = mockk<EmailChannel>(relaxed = true)

    private val attributes: Attributes = mapOf("email.subject" to JsonPrimitive("Test subject"))

    private fun makeConversation() = EmailConversation(
        attributes = attributes,
        channel = channel,
        recipientAddress = "client@example.com",
    )

    private fun makeDocumentAttachment(name: String, bytes: ByteArray): Attachment.Document {
        val attachChannel = mockk<Channel>(relaxed = true)
        return object : Attachment.Document {
            override val channel = attachChannel
            override val attributes: Attributes = emptyMap()
            override val contentType = io.ktor.http.ContentType.Application.OctetStream
            override val fileName = name
            override val fileSize = bytes.size.toLong()
            override val contentSource get() = Buffer().also { it.write(bytes) }
        }
    }

    private fun makeStickerAttachment(): Attachment.Sticker {
        val attachChannel = mockk<Channel>(relaxed = true)
        return object : Attachment.Sticker {
            override val channel = attachChannel
            override val attributes: Attributes = emptyMap()
            override val contentType = io.ktor.http.ContentType.Image.PNG
            override val fileName = "sticker.png"
            override val fileSize = 0L
            override val height = 512
            override val width = 512
            override val contentSource: kotlinx.io.Source get() = Buffer()
        }
    }

    @Test
    fun `should build multipart_alternative when message has no attachments`() {
        val conversation = makeConversation()
        val content = conversation.buildContent("Hello", emptyList())
        assertEquals("alternative", content.contentType.removePrefix("multipart/").substringBefore(";").trim())
    }

    @Test
    fun `should build multipart_alternative when message has only sticker attachments`() {
        val conversation = makeConversation()
        val content = conversation.buildContent("Hello", listOf(makeStickerAttachment()))
        assertEquals("alternative", content.contentType.removePrefix("multipart/").substringBefore(";").trim())
    }

    @Test
    fun `should build multipart_mixed when message has non-sticker attachments`() {
        val conversation = makeConversation()
        val attachment = makeDocumentAttachment("report.pdf", byteArrayOf(1, 2, 3))
        val content = conversation.buildContent("Hello", listOf(attachment))
        assertEquals("mixed", content.contentType.removePrefix("multipart/").substringBefore(";").trim())
    }

    @Test
    fun `should embed alternative part and attachment parts in mixed multipart`() {
        val conversation = makeConversation()
        val attachment = makeDocumentAttachment("file.bin", byteArrayOf(10, 20, 30))
        val content = conversation.buildContent("Hi", listOf(attachment))

        assertEquals(2, content.count, "Mixed multipart should contain 2 parts: alternative + 1 attachment")

        val firstPart = content.getBodyPart(0)
        val innerContent = firstPart.content
        check(innerContent is Multipart) { "First body part should wrap a multipart/alternative" }
        assertEquals(
            "alternative",
            innerContent.contentType.removePrefix("multipart/").substringBefore(";").trim(),
        )
    }

    @Test
    fun `should skip sticker but include document in mixed multipart`() {
        val conversation = makeConversation()
        val sticker = makeStickerAttachment()
        val document = makeDocumentAttachment("doc.txt", byteArrayOf(5, 6))
        val content = conversation.buildContent("Hi", listOf(sticker, document))

        // mixed with 2 parts: alternative wrapper + 1 attachment (sticker skipped)
        assertEquals("mixed", content.contentType.removePrefix("multipart/").substringBefore(";").trim())
        assertEquals(2, content.count, "Only the document should be attached, sticker skipped")
    }

}
