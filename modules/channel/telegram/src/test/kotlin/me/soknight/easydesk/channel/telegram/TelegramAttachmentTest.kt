package me.soknight.easydesk.channel.telegram

import io.ktor.http.ContentType
import io.mockk.mockk
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.model.Attachment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TelegramAttachmentTest {

    private val channel = mockk<Channel>(relaxed = true)

    @Test
    fun `photo has kind PHOTO and provides content source when bytes present`() {
        val bytes = byteArrayOf(1, 2, 3)
        val attachment = TelegramAttachment.Photo("file_id", bytes, 100L, 100, 100, channel)
        assertEquals(Attachment.Kind.PHOTO, attachment.kind)
        // accessing contentSource should not throw
        attachment.contentSource
    }

    @Test
    fun `content source throws when no bytes`() {
        val attachment = TelegramAttachment.Photo("file_id", null, 30_000_000L, 100, 100, channel)
        assertFailsWith<UnsupportedOperationException> { attachment.contentSource }
    }

    @Test
    fun `file id stored in attributes`() {
        val attachment = TelegramAttachment.Document("abc123", null, "doc.pdf", ContentType.Application.Pdf, null, channel)
        assertEquals("abc123", (attachment.attributes["telegram.file_id"] as JsonPrimitive).content)
    }

    @Test
    fun `sticker never has bytes and has kind STICKER`() {
        val sticker = TelegramAttachment.Sticker("sticker_id", 12_000L, 512, 512, channel)
        assertNull(sticker.bytes)
        assertEquals(Attachment.Kind.STICKER, sticker.kind)
    }

}
