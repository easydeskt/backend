package me.soknight.easydesk.channel.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.AnimationContent
import dev.inmo.tgbotapi.types.message.content.AudioContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.types.message.content.VoiceContent
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import kotlin.time.Duration.Companion.seconds

/**
 * Stateless utility for parsing Telegram message content into [TelegramAttachment] instances.
 *
 * Extracted from `ChannelProviderDelegate` so that both the channel provider and the supervisor
 * reply handler can reuse the same download-and-parse logic without duplication.
 *
 * Files that exceed the [DOWNLOAD_LIMIT] are referenced by `file_id` only — [TelegramAttachment.bytes]
 * will be `null` for those. Download failures are logged as warnings; the attachment is still
 * created and forwarded by `file_id`.
 */
object TelegramAttachmentParser {

    private const val DOWNLOAD_LIMIT = 20L * 1024L * 1024L

    private val logger = getLogger()

    /**
     * Parses the media content of [message] into a list of [TelegramAttachment]s.
     *
     * Returns an empty list for message types that carry no downloadable media
     * (e.g. text-only, polls, location).
     *
     * @param message the incoming Telegram content message
     * @param bot the bot used to download file bytes
     * @param channel the channel this attachment originates from
     */
    suspend fun parse(
        message: ContentMessage<*>,
        bot: TelegramBot,
        channel: TelegramChannel,
    ): List<TelegramAttachment> = when (val content = message.content) {
        is AnimationContent -> {
            val anim = content.media
            val fileSize = anim.fileSize?.bytes?.toLong()
            val bytes = downloadIfWithinLimit(bot, anim, fileSize)
            val ct = anim.mimeType?.raw?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                ?: ContentType.Image.GIF
            listOf(TelegramAttachment.Document(
                fileId = anim.fileId.fileId,
                bytes = bytes,
                fileName = anim.fileName ?: "animation.gif",
                contentType = ct,
                fileSize = fileSize,
                channel = channel,
            ))
        }
        is AudioContent -> {
            val audio = content.media
            val fileSize = audio.fileSize?.bytes?.toLong()
            val bytes = downloadIfWithinLimit(bot, audio, fileSize)
            listOf(TelegramAttachment.Audio(
                fileId = audio.fileId.fileId,
                bytes = bytes,
                fileName = audio.fileName ?: "audio.mp3",
                fileSize = fileSize,
                duration = (audio.duration ?: 0L).seconds,
                performer = audio.performer,
                title = audio.title,
                channel = channel,
            ))
        }
        is DocumentContent -> {
            val doc = content.media
            val fileSize = doc.fileSize?.bytes?.toLong()
            val bytes = downloadIfWithinLimit(bot, doc, fileSize)
            val ct = doc.mimeType?.raw?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                ?: ContentType.Application.OctetStream
            listOf(TelegramAttachment.Document(
                fileId = doc.fileId.fileId,
                bytes = bytes,
                fileName = doc.fileName ?: "document",
                contentType = ct,
                fileSize = fileSize,
                channel = channel,
            ))
        }
        is PhotoContent -> {
            val photo = content.media
            val fileSize = photo.fileSize?.bytes?.toLong()
            val bytes = downloadIfWithinLimit(bot, photo, fileSize)
            listOf(TelegramAttachment.Photo(
                fileId = photo.fileId.fileId,
                bytes = bytes,
                fileSize = fileSize,
                height = photo.height,
                width = photo.width,
                channel = channel,
            ))
        }
        is StickerContent -> {
            val sticker = content.media
            listOf(TelegramAttachment.Sticker(
                fileId = sticker.fileId.fileId,
                fileSize = sticker.fileSize?.bytes?.toLong(),
                height = sticker.height,
                width = sticker.width,
                channel = channel,
            ))
        }
        is VideoContent -> {
            val vid = content.media
            val fileSize = vid.fileSize?.bytes?.toLong()
            val bytes = downloadIfWithinLimit(bot, vid, fileSize)
            listOf(TelegramAttachment.Video(
                fileId = vid.fileId.fileId,
                bytes = bytes,
                fileName = vid.fileName ?: "video.mp4",
                fileSize = fileSize,
                duration = (vid.duration ?: 0L).seconds,
                height = vid.height,
                width = vid.width,
                channel = channel,
            ))
        }
        is VoiceContent -> {
            val voice = content.media
            val fileSize = voice.fileSize?.bytes?.toLong()
            val bytes = downloadIfWithinLimit(bot, voice, fileSize)
            listOf(TelegramAttachment.Voice(
                fileId = voice.fileId.fileId,
                bytes = bytes,
                fileSize = fileSize,
                duration = (voice.duration ?: 0L).seconds,
                channel = channel,
            ))
        }
        else -> emptyList()
    }

    internal suspend fun downloadIfWithinLimit(
        bot: TelegramBot,
        file: TelegramMediaFile,
        fileSize: Long?,
    ): ByteArray? {
        if (fileSize != null && fileSize > DOWNLOAD_LIMIT) return null
        return runCatching { bot.downloadFile(file) }
            .onFailure {
                if (it is CancellationException) throw it
                logger.warn(it) { "Failed to download Telegram file ${file.fileId.fileId}" }
            }
            .getOrNull()
    }

}
