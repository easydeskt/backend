package me.soknight.easydesk.channel.telegram

import io.ktor.http.ContentType
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Attachment
import kotlin.time.Duration

/**
 * In-memory transient attachment produced during Telegram message receive.
 *
 * [bytes] is `null` when the file exceeds the 20 MB Bot API download limit,
 * or when the attachment type is never downloaded (e.g. [Sticker]).
 */
sealed class TelegramAttachment(
    val fileId: String,
    val bytes: ByteArray?,
    override val contentType: ContentType,
    override val fileName: String,
    override val fileSize: Long?,
    override val channel: Channel,
) : Attachment {

    override val attributes: Attributes
        get() = mapOf("telegram.file_id" to JsonPrimitive(fileId))

    override val contentSource: Source
        get() = bytes?.let { Buffer().apply { write(it) } }
            ?: throw UnsupportedOperationException("No cached bytes for file_id=$fileId (file > 20 MB or download skipped)")

    class Audio(
        fileId: String,
        bytes: ByteArray?,
        fileName: String,
        fileSize: Long?,
        override val duration: Duration,
        override val performer: String?,
        override val title: String?,
        channel: Channel,
    ) : TelegramAttachment(fileId, bytes, ContentType.Audio.MPEG, fileName, fileSize, channel),
        Attachment.Audio

    class Document(
        fileId: String,
        bytes: ByteArray?,
        fileName: String,
        contentType: ContentType,
        fileSize: Long?,
        channel: Channel,
    ) : TelegramAttachment(fileId, bytes, contentType, fileName, fileSize, channel),
        Attachment.Document

    class Photo(
        fileId: String,
        bytes: ByteArray?,
        fileSize: Long?,
        override val height: Int,
        override val width: Int,
        channel: Channel,
    ) : TelegramAttachment(fileId, bytes, ContentType.Image.JPEG, "photo.jpg", fileSize, channel),
        Attachment.Photo

    class Sticker(
        fileId: String,
        fileSize: Long?,
        override val height: Int,
        override val width: Int,
        channel: Channel,
    ) : TelegramAttachment(fileId, null, ContentType.Image.Any, "sticker.webp", fileSize, channel),
        Attachment.Sticker

    class Video(
        fileId: String,
        bytes: ByteArray?,
        fileName: String,
        fileSize: Long?,
        override val duration: Duration,
        override val height: Int,
        override val width: Int,
        channel: Channel,
    ) : TelegramAttachment(fileId, bytes, ContentType.Video.MP4, fileName, fileSize, channel),
        Attachment.Video

    class Voice(
        fileId: String,
        bytes: ByteArray?,
        fileSize: Long?,
        override val duration: Duration,
        channel: Channel,
    ) : TelegramAttachment(fileId, bytes, ContentType.Audio.OGG, "voice.ogg", fileSize, channel),
        Attachment.Voice

}
