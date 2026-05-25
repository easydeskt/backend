package me.soknight.easydesk.channel.telegram.internal

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.AnimationContent
import dev.inmo.tgbotapi.types.message.content.AudioContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.types.message.content.VoiceContent
import dev.inmo.tgbotapi.types.toChatId
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.telegram.TelegramAttachment
import me.soknight.easydesk.channel.telegram.TelegramBrand
import me.soknight.easydesk.channel.telegram.TelegramChannel
import me.soknight.easydesk.channel.telegram.TelegramIdentity
import me.soknight.easydesk.channel.telegram.config.TelegramConfig
import me.soknight.easydesk.channel.telegram.domain.TelegramConversation
import me.soknight.easydesk.channel.telegram.domain.TelegramMessage
import me.soknight.easydesk.channel.telegram.event.TelegramMessageReceived
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.info
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class ChannelProviderDelegate(
    private val logger: Logger,
    private val channelRepository: ChannelRepository,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val activeBots = ConcurrentHashMap<Long, Pair<TelegramChannel, TelegramBot>>()
    private val pollingJobs = ConcurrentHashMap<Long, Job>()

    // -------------- INTERNAL API -------------------------------------------------------------------------------------

    fun getBot(serviceChannelId: Long): TelegramBot? =
        activeBots[serviceChannelId]?.second

    fun getBotForChannel(channel: TelegramChannel): TelegramBot? =
        activeBots.values.firstOrNull { it.first === channel }?.second

    fun getChannel(serviceChannelId: Long): TelegramChannel? =
        activeBots[serviceChannelId]?.first

    suspend fun start(scope: CoroutineScope, eventBus: EventBus) {
        val existing = channelRepository.findByBrand(TelegramBrand.identifier, enabledOnly = false)
        if (existing.isEmpty()) {
            val token = System.getenv("TELEGRAM_CHANNEL_BOT_TOKEN")
            if (token != null) {
                val configJson = buildJsonObject { put("token", "\${TELEGRAM_CHANNEL_BOT_TOKEN}") }
                channelRepository.create(brand = TelegramBrand.identifier, displayName = "Telegram", config = configJson)
                logger.info { "Auto-bootstrapped Telegram channel from TELEGRAM_CHANNEL_BOT_TOKEN" }
            }
        }
        val serviceChannels = channelRepository.findByBrand(TelegramBrand.identifier, enabledOnly = true)
        logger.info { "Starting ${serviceChannels.size} Telegram channel(s)" }
        for (serviceChannel in serviceChannels) {
            startChannel(serviceChannel, scope, eventBus)
        }
    }

    suspend fun stop() {
        logger.info { "Stopping ${pollingJobs.size} Telegram channel(s)" }

        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
        activeBots.clear()
    }

    val channels: List<Channel>
        get() = activeBots.values.map { it.first }

    // -------------- PRIVATE IMPLEMENTATION ---------------------------------------------------------------------------

    private suspend fun startChannel(serviceChannel: me.soknight.easydesk.service.channels.data.domain.Channel, scope: CoroutineScope, eventBus: EventBus) {
        val resolvedJson = resolveEnvVars(serviceChannel.config.toString())
        val config = json.decodeFromString<TelegramConfig>(resolvedJson)
        val channel = TelegramChannel(serviceChannel.displayName, serviceChannel.displayName, config)
        val bot = telegramBot(config.token)
        activeBots[serviceChannel.id] = channel to bot
        val job = bot.buildBehaviourWithLongPolling(scope) {
            onContentMessage { message ->
                try {
                    val chat = message.chat
                    if (chat !is PrivateChat) return@onContentMessage
                    val from = (message as? OptionallyFromUserMessage)?.from ?: return@onContentMessage
                    val identity = TelegramIdentity(from.id)
                    val conversation = TelegramConversation(
                        attributes = emptyMap(),
                        bot = bot,
                        channel = channel,
                        userChatId = chat.id.toChatId(),
                    )
                    val attachments = buildAttachments(message, bot, channel)
                    val telegramMessage = TelegramMessage(
                        conversation = conversation,
                        messageId = message.messageId,
                        plainText = (message.content as? TextContent)?.text,
                        receiver = ChannelActor.System,
                        sender = identity,
                        attachments = attachments,
                    )
                    eventBus.publish(
                        TelegramMessageReceived(
                            conversation = conversation,
                            message = telegramMessage,
                            timestamp = Instant.fromEpochMilliseconds(message.date.unixMillisLong),
                        )
                    )
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to handle incoming Telegram message: ${e.message}" }
                }
            }
        }
        pollingJobs[serviceChannel.id] = job
        logger.info { "Started Telegram bot for channel '${serviceChannel.displayName}' (id=${serviceChannel.id})" }
    }

    private fun resolveEnvVars(json: String): String =
        Regex("""\$\{([^}]+)}""").replace(json) { result ->
            System.getenv(result.groupValues[1]) ?: result.value
        }

    private suspend fun buildAttachments(
        message: ContentMessage<*>,
        bot: TelegramBot,
        channel: TelegramChannel,
    ): List<TelegramAttachment> {
        return when (val content = message.content) {
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
    }

    private suspend fun downloadIfWithinLimit(
        bot: TelegramBot,
        file: TelegramMediaFile,
        fileSize: Long?,
    ): ByteArray? {
        if (fileSize != null && fileSize > TELEGRAM_DOWNLOAD_LIMIT) return null
        return runCatching { bot.downloadFile(file) }
            .onFailure { logger.warn(it) { "Failed to download Telegram file ${file.fileId}" } }
            .getOrNull()
    }

    companion object {
        private const val TELEGRAM_DOWNLOAD_LIMIT = 20L * 1024L * 1024L
    }

}
