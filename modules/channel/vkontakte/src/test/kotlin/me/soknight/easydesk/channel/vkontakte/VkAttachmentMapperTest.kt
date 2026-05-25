package me.soknight.easydesk.channel.vkontakte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.vkontakte.vk.model.VkAttachment

class VkAttachmentMapperTest {

    private val channel = mockk<Channel>(relaxed = true)

    private fun mockHttpClient(bytes: ByteArray): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { respond(bytes, HttpStatusCode.OK, headersOf()) }
            }
        }

    @Test
    fun `should_returnAttachmentPhoto_when_photoAttachmentMapped`() = runTest {
        val photoBytes = byteArrayOf(1, 2, 3, 4, 5)
        val httpClient = mockHttpClient(photoBytes)
        val vkPhoto = VkAttachment.Photo(
            id = 100,
            ownerId = 42L,
            sizes = listOf(
                VkAttachment.Photo.Size(height = 200, type = "x", url = "https://example.com/photo.jpg", width = 300),
                VkAttachment.Photo.Size(height = 100, type = "m", url = "https://example.com/photo_m.jpg", width = 150),
            ),
        )

        val result = VkAttachmentMapper.map(vkPhoto, channel, httpClient)

        assertIs<Attachment.Photo>(result)
        assertEquals(Attachment.Kind.PHOTO, result.kind)
        assertEquals(300, result.width)
        assertEquals(200, result.height)
        assertEquals(photoBytes.toList(), result.contentSource.readByteArray().toList())
    }

    @Test
    fun `should_returnNull_when_stickerAttachmentMapped`() = runTest {
        val httpClient = mockHttpClient(byteArrayOf())
        val vkSticker = VkAttachment.Sticker(
            images = listOf(VkAttachment.Sticker.Image(height = 512, url = "https://example.com/sticker.png", width = 512)),
            stickerId = 9001,
        )

        val result = VkAttachmentMapper.map(vkSticker, channel, httpClient)

        assertNull(result)
    }

    @Test
    fun `should_returnAttachmentVoice_when_audioMessageAttachmentMapped`() = runTest {
        val voiceBytes = byteArrayOf(10, 20, 30)
        val httpClient = mockHttpClient(voiceBytes)
        val vkAudioMessage = VkAttachment.AudioMessage(
            duration = 7,
            id = 55,
            linkMp3 = "https://example.com/voice.mp3",
            ownerId = 42L,
        )

        val result = VkAttachmentMapper.map(vkAudioMessage, channel, httpClient)

        assertIs<Attachment.Voice>(result)
        assertEquals(Attachment.Kind.VOICE, result.kind)
        assertEquals(7, result.duration.inWholeSeconds.toInt())
        assertEquals(voiceBytes.toList(), result.contentSource.readByteArray().toList())
    }

    @Test
    fun `should_returnNull_when_audioAttachmentMapped`() = runTest {
        val httpClient = mockHttpClient(byteArrayOf())
        val vkAudio = VkAttachment.Audio(
            artist = "Artist",
            id = 1,
            ownerId = 42L,
            title = "Track",
        )

        val result = VkAttachmentMapper.map(vkAudio, channel, httpClient)

        assertNull(result)
    }

    @Test
    fun `should_returnAttachmentDocument_when_documentAttachmentWithUrlMapped`() = runTest {
        val docBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF magic
        val httpClient = mockHttpClient(docBytes)
        val vkDocument = VkAttachment.Document(
            ext = "pdf",
            id = 77,
            ownerId = 42L,
            title = "Report",
            url = "https://example.com/report.pdf",
        )

        val result = VkAttachmentMapper.map(vkDocument, channel, httpClient)

        assertIs<Attachment.Document>(result)
        assertEquals(Attachment.Kind.DOCUMENT, result.kind)
        assertEquals(docBytes.toList(), result.contentSource.readByteArray().toList())
    }

    @Test
    fun `should_returnNull_when_documentAttachmentHasNoUrl`() = runTest {
        val httpClient = mockHttpClient(byteArrayOf())
        val vkDocument = VkAttachment.Document(
            ext = "pdf",
            id = 77,
            ownerId = 42L,
            title = "Report",
            url = null,
        )

        val result = VkAttachmentMapper.map(vkDocument, channel, httpClient)

        assertNull(result)
    }

    @Test
    fun `should_returnAttachmentDocumentWithPlayerUrl_when_videoAttachmentMapped`() = runTest {
        val httpClient = mockHttpClient(byteArrayOf())
        val vkVideo = VkAttachment.Video(id = 11, ownerId = 42L, title = "My Video")

        val result = VkAttachmentMapper.map(vkVideo, channel, httpClient)

        assertIs<Attachment.Document>(result)
        assertEquals(Attachment.Kind.DOCUMENT, result.kind)
        val playerUrl = result.attributes["vk.player_url"]
        assertEquals("https://vk.com/video42_11", (playerUrl as JsonPrimitive).content)
    }

}
