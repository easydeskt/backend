package me.soknight.easydesk.channel.email

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.dsl.channelConfigSchema
import me.soknight.easydesk.channel.email.config.EmailConfig
import me.soknight.easydesk.channel.email.domain.EmailConversation
import me.soknight.easydesk.channel.email.domain.EmailMessage
import me.soknight.easydesk.channel.email.event.EmailMessageReceived
import me.soknight.easydesk.channel.email.internal.ImapPoller
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.info
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.channels.data.domain.Channel as ServiceChannel
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Single(binds = [ChannelProvider::class])
internal class EmailProviderBinding : ChannelProvider by EmailProvider

/**
 * [ChannelProvider] implementation for Email (IMAP/SMTP).
 *
 * Loads configured mailboxes from the database at [start], launches one [ImapPoller]
 * per channel, and stops all pollers on [stop].
 *
 * @see EmailBrand
 * @see EmailIdentity
 */
object EmailProvider : ChannelProvider, KoinComponent {

    private val channelRepository by lazy { get<ChannelRepository>() }
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = getLogger()

    private val activeChannels = ConcurrentHashMap<Long, EmailChannel>()
    private val pollers = ConcurrentHashMap<Long, ImapPoller>()

    override val brand get() = EmailBrand

    override val channels: List<Channel>
        get() = activeChannels.values.toList()

    override val configSchema by channelConfigSchema {
        text("reply_to")

        section("from") {
            text("address", required = true)
            text("name")
        }

        section("imap") {
            text("host")
            int("port", defaultValue = 993)
            text("username")
            password("password")
            text("folder", defaultValue = "INBOX")
            boolean("use_ssl", defaultValue = true)
            int("poll_interval_seconds", defaultValue = 30)
        }

        section("smtp") {
            text("host")
            int("port", defaultValue = 587)
            text("username")
            password("password")
        }

        order("from", "imap", "smtp", "reply_to")
    }

    override suspend fun start(scope: CoroutineScope, eventBus: EventBus) {
        val serviceChannels = channelRepository.findByBrand(EmailBrand.identifier, enabledOnly = true)
        logger.info { "Starting ${serviceChannels.size} email channel(s)" }
        serviceChannels.forEach { startChannel(it, scope, eventBus) }
    }

    override suspend fun stop() {
        logger.info { "Stopping ${pollers.size} email channel(s)" }
        pollers.values.forEach { it.stop() }
        pollers.clear()
        activeChannels.clear()
    }

    private fun resolveEnvVars(text: String): String =
        Regex("""\$\{([^}]+)}""").replace(text) { result ->
            System.getenv(result.groupValues[1]) ?: result.value
        }

    private fun startChannel(serviceChannel: ServiceChannel, scope: CoroutineScope, eventBus: EventBus) {
        val resolvedJson = resolveEnvVars(serviceChannel.config.toString())
        val config = json.decodeFromString<EmailConfig>(resolvedJson)
        val channel = EmailChannel(serviceChannel.displayName, serviceChannel.displayName, config)
        val poller = ImapPoller(channel, logger)
        activeChannels[serviceChannel.id] = channel
        pollers[serviceChannel.id] = poller
        poller.start(scope) { message, conversation, timestamp ->
            handleIncoming(message, conversation, timestamp, eventBus)
        }
        logger.info { "Started email channel '${serviceChannel.displayName}' (id=${serviceChannel.id})" }
    }

    private suspend fun handleIncoming(
        message: EmailMessage,
        conversation: EmailConversation,
        timestamp: Instant,
        eventBus: EventBus,
    ) {
        try {
            eventBus.publish(EmailMessageReceived(conversation, message, timestamp))
        } catch (e: Exception) {
            logger.warn(e) { "Failed to publish EmailMessageReceived for '${message.nativeId}': ${e.message}" }
        }
    }

}
