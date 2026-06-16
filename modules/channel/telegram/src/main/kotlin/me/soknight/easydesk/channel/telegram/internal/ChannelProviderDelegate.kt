package me.soknight.easydesk.channel.telegram.internal

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.telegram.TelegramAttachment
import me.soknight.easydesk.channel.telegram.TelegramAttachmentParser
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
import me.soknight.easydesk.service.vault.resolver.SecretReferenceResolver
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant

internal class ChannelProviderDelegate(
    private val channelRepository: ChannelRepository,
    private val logger: Logger,
    private val secretReferenceResolver: SecretReferenceResolver,
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
        val withVaultSecrets = secretReferenceResolver.resolve(serviceChannel.config.toString())
        val resolvedJson = resolveEnvVars(withVaultSecrets)
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
                    if (e is CancellationException) throw e
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
    ): List<TelegramAttachment> = TelegramAttachmentParser.parse(message, bot, channel)

}
