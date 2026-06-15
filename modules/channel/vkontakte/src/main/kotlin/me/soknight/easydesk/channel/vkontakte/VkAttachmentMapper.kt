package me.soknight.easydesk.channel.vkontakte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.fromFileExtension
import io.ktor.http.isSuccess
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.vkontakte.vk.model.VkAttachment

/**
 * Maps [VkAttachment] subtypes to channel-api [Attachment] instances.
 *
 * Each supported type downloads bytes via [httpClient] and produces an
 * anonymous [Attachment] implementation. Unsupported types (e.g. [VkAttachment.Sticker],
 * [VkAttachment.Audio]) return `null` and are silently dropped by the caller.
 *
 * Download failures are swallowed via [runCatching] so that a single broken
 * attachment does not abort the whole message.
 *
 * @see VkAttachment
 * @see Attachment
 */
object VkAttachmentMapper {

    /**
     * Maps a single [vkAttachment] to a channel-api [Attachment], or returns `null`
     * if the attachment type is unsupported or the download failed.
     *
     * @param vkAttachment the VK attachment to map
     * @param channel the channel that received the attachment
     * @param httpClient the HTTP client used to download attachment bytes
     */
    suspend fun map(vkAttachment: VkAttachment, channel: Channel, httpClient: HttpClient): Attachment? =
        when (vkAttachment) {
            is VkAttachment.Audio -> null // no public download URL exposed in VK API v5
            is VkAttachment.AudioMessage -> mapAudioMessage(vkAttachment, channel, httpClient)
            is VkAttachment.Document -> mapDocument(vkAttachment, channel, httpClient)
            is VkAttachment.Graffiti -> mapGraffiti(vkAttachment, channel, httpClient)
            is VkAttachment.Link -> null
            is VkAttachment.Market -> null
            is VkAttachment.Photo -> mapPhoto(vkAttachment, channel, httpClient)
            is VkAttachment.Sticker -> null
            is VkAttachment.Unknown -> null
            is VkAttachment.Video -> mapVideo(vkAttachment, channel)
            is VkAttachment.Wall -> null
        }

    // ── private helpers ───────────────────────────────────────────────────────

    private suspend fun download(httpClient: HttpClient, url: String): ByteArray? =
        runCatching {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) null else response.body<ByteArray>()
        }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()

    private suspend fun mapAudioMessage(
        vkAttachment: VkAttachment.AudioMessage,
        channel: Channel,
        httpClient: HttpClient,
    ): Attachment? {
        val bytes = download(httpClient, vkAttachment.linkMp3) ?: return null
        val duration = vkAttachment.duration.seconds
        return object : Attachment.Voice {
            override val attributes: Attributes = mapOf("vk.url" to JsonPrimitive(vkAttachment.linkMp3))
            override val channel: Channel = channel
            override val contentSource: Source get() = Buffer().also { it.write(bytes) }
            override val contentType: ContentType = ContentType.Audio.MPEG
            override val duration = duration
            override val fileName: String = "voice.mp3"
            override val fileSize: Long = bytes.size.toLong()
        }
    }

    private suspend fun mapDocument(
        vkAttachment: VkAttachment.Document,
        channel: Channel,
        httpClient: HttpClient,
    ): Attachment? {
        val url = vkAttachment.url ?: return null
        val bytes = download(httpClient, url) ?: return null
        val contentType = ContentType.fromFileExtension(vkAttachment.ext)
            .firstOrNull()
            ?.takeUnless { it == ContentType.Any }
            ?: ContentType.Application.OctetStream
        val fileName = "${vkAttachment.title}.${vkAttachment.ext}".trimStart('.')
        return object : Attachment.Document {
            override val attributes: Attributes = mapOf("vk.url" to JsonPrimitive(url))
            override val channel: Channel = channel
            override val contentSource: Source get() = Buffer().also { it.write(bytes) }
            override val contentType: ContentType = contentType
            override val fileName: String = fileName
            override val fileSize: Long = bytes.size.toLong()
        }
    }

    private suspend fun mapGraffiti(
        vkAttachment: VkAttachment.Graffiti,
        channel: Channel,
        httpClient: HttpClient,
    ): Attachment? {
        val bytes = download(httpClient, vkAttachment.url) ?: return null
        return object : Attachment.Photo {
            override val attributes: Attributes = mapOf("vk.url" to JsonPrimitive(vkAttachment.url))
            override val channel: Channel = channel
            override val contentSource: Source get() = Buffer().also { it.write(bytes) }
            override val contentType: ContentType = ContentType.Image.PNG
            override val fileName: String = "graffiti.png"
            override val fileSize: Long = bytes.size.toLong()
            override val height: Int = 0
            override val width: Int = 0
        }
    }

    private suspend fun mapPhoto(
        vkAttachment: VkAttachment.Photo,
        channel: Channel,
        httpClient: HttpClient,
    ): Attachment? {
        val best = vkAttachment.largest ?: return null
        val bytes = download(httpClient, best.url) ?: return null
        val height = best.height
        val width = best.width
        return object : Attachment.Photo {
            override val attributes: Attributes = mapOf("vk.url" to JsonPrimitive(best.url))
            override val channel: Channel = channel
            override val contentSource: Source get() = Buffer().also { it.write(bytes) }
            override val contentType: ContentType = ContentType.Image.JPEG
            override val fileName: String = "photo.jpg"
            override val fileSize: Long = bytes.size.toLong()
            override val height: Int = height
            override val width: Int = width
        }
    }

    private fun mapVideo(vkAttachment: VkAttachment.Video, channel: Channel): Attachment {
        val playerUrl = "https://vk.com/video${vkAttachment.ownerId}_${vkAttachment.id}"
        return object : Attachment.Document {
            override val attributes: Attributes = mapOf("vk.player_url" to JsonPrimitive(playerUrl))
            override val channel: Channel = channel
            override val contentSource: Source
                get() = Buffer()  // no bytes available; use vk.player_url attribute instead
            override val contentType: ContentType = ContentType("video", "mp4")
            override val fileName: String = "${vkAttachment.title}.mp4"
            override val fileSize: Long? = null
        }
    }

}
