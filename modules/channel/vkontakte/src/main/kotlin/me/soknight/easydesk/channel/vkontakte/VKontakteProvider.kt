package me.soknight.easydesk.channel.vkontakte

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.Application
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.dsl.channelConfigSchema
import me.soknight.easydesk.channel.vkontakte.config.VKontakteConfig
import me.soknight.easydesk.channel.vkontakte.event.VKontakteMessageReceived
import me.soknight.easydesk.channel.vkontakte.vk.VkBehaviourContext
import me.soknight.easydesk.channel.vkontakte.vk.VkBot
import me.soknight.easydesk.channel.vkontakte.vk.buildBehaviourWithCallbackApi
import me.soknight.easydesk.channel.vkontakte.vk.buildBehaviourWithLongPolling
import me.soknight.easydesk.channel.vkontakte.vk.onMessageNew
import me.soknight.easydesk.channel.vkontakte.vk.vkBot
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.info
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Single(binds = [ChannelProvider::class])
internal class VKontakteProviderBinding : ChannelProvider by VKontakteProvider

/**
 * [ChannelProvider] implementation for VKontakte.
 *
 * Singleton entry point for the VKontakte channel. Uses [VKontakteBrand]
 * as its platform descriptor.
 *
 * @see VKontakteBrand
 * @see VKontakteIdentity
 */
object VKontakteProvider : ChannelProvider, KoinComponent {

    private val channelRepository by lazy { get<ChannelRepository>() }
    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 60_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = getLogger()

    private val activeChannels = ConcurrentHashMap<Long, VKontakteChannel>()
    private val activeBots = ConcurrentHashMap<Long, VkBot>()
    private val lpJobs = ConcurrentHashMap<Long, Job>()

    override val brand get() = VKontakteBrand

    override val channels: List<Channel>
        get() = activeChannels.values.toList()

    override val configSchema by channelConfigSchema {
        long("group_id", required = true)
        password("token", required = true)

        section("longpoll") {
            boolean("enabled", defaultValue = true)
            int("wait_seconds", defaultValue = 30)
        }

        section("callback") {
            boolean("enabled")
            text("confirmation_code", required = true)
            text("listen_path", defaultValue = "/vkontakte/callback")
            password("secret")
        }
    }

    override suspend fun start(scope: CoroutineScope, eventBus: EventBus) {
        val serviceChannels = channelRepository.findByBrand(VKontakteBrand.identifier, enabledOnly = true)
        logger.info { "Starting ${serviceChannels.size} VKontakte channel(s)" }

        for (serviceChannel in serviceChannels) {
            val resolvedJson = resolveEnvVars(serviceChannel.config.toString())
            val config = json.decodeFromString<VKontakteConfig>(resolvedJson)
            val channel = VKontakteChannel(serviceChannel.displayName, serviceChannel.displayName, config)
            val bot = vkBot(config.groupId, config.token)

            activeChannels[serviceChannel.id] = channel
            activeBots[serviceChannel.id] = bot

            // Long Poll takes priority; both enabled simultaneously is not supported
            when {
                config.longpoll?.isEnabled == true -> {
                    val job = bot.buildBehaviourWithLongPolling(scope) {
                        registerHandlers(channel, eventBus)
                    }
                    lpJobs[serviceChannel.id] = job
                    logger.info { "Started VK Long Poll for channel '${serviceChannel.displayName}'" }
                }

                config.callback?.isEnabled == true -> {
                    val application = scope as? Application
                        ?: error("Callback mode requires Ktor Application scope")
                    bot.buildBehaviourWithCallbackApi(
                        application = application,
                        confirmationCode = config.callback.confirmationCode
                            ?: error("callback.confirmation_code is required"),
                        listenPath = config.callback.listenPath,
                        secret = config.callback.secret,
                    ) {
                        registerHandlers(channel, eventBus)
                    }
                    logger.info { "Registered VK Callback route for channel '${serviceChannel.displayName}'" }
                }
            }
        }
    }

    override suspend fun stop() {
        logger.info { "Stopping ${lpJobs.size} VKontakte Long Poll job(s)" }
        lpJobs.values.forEach { it.cancel() }
        activeBots.clear()
        lpJobs.clear()
        activeChannels.clear()
        httpClient.close()
    }

    fun getBot(serviceChannelId: Long): VkBot? = activeBots[serviceChannelId]

    fun getBotForChannel(channel: VKontakteChannel): VkBot? =
        activeBots.entries.firstOrNull { (key, _) -> activeChannels[key] === channel }?.value

    fun getChannel(serviceChannelId: Long): VKontakteChannel? = activeChannels[serviceChannelId]

    // ── private helpers ───────────────────────────────────────────────────────

    private fun VkBehaviourContext.registerHandlers(channel: VKontakteChannel, eventBus: EventBus) {
        onMessageNew { event ->
            try {
                val msg = event.message
                val identity = VKontakteIdentity(msg.fromId)
                val conversation = VKontakteConversation(bot = bot, channel = channel, peerId = msg.peerId)
                val attachments = msg.attachments.mapNotNull {
                    VkAttachmentMapper.map(it, channel, httpClient)
                }
                val message = VKontakteMessage(
                    attachments = attachments,
                    conversation = conversation,
                    receiver = ChannelActor.System,
                    sender = identity,
                    vkMessage = msg,
                )
                eventBus.publish(
                    VKontakteMessageReceived(
                        conversation = conversation,
                        message = message,
                        timestamp = Instant.fromEpochSeconds(msg.date),
                    )
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to handle VKontakte message: ${e.message}" }
            }
        }
    }

    private fun resolveEnvVars(text: String): String =
        Regex("""\$\{([^}]+)}""").replace(text) { result ->
            System.getenv(result.groupValues[1]) ?: result.value
        }

}
