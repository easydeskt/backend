package me.soknight.easydesk.channel.api

import kotlinx.coroutines.CoroutineScope
import me.soknight.easydesk.channel.api.config.ConfigSchema
import me.soknight.easydesk.core.event.EventBus

/**
 * A concrete channel implementation that bridges the helpdesk system
 * with an external messaging platform.
 *
 * Each supported platform (Telegram, VKontakte, Email, etc.) provides its
 * own [ChannelProvider] instance, registered via Koin, that exposes the
 * platform's [brand] metadata and manages its lifecycle.
 *
 * ### Channels
 *
 * A single provider can serve multiple connections of the same type
 * (e.g., several Telegram bots or email inboxes). Each connection is
 * represented by a [Channel] instance. The [channels] property returns
 * all currently registered channels for this provider.
 *
 * ### Lifecycle
 *
 * The provider is stateless until [start] is called. Once started, it
 * connects to the platform for every registered [Channel] (via polling,
 * webhook, IMAP, etc.), parses incoming updates, and publishes
 * [events][me.soknight.easydesk.channel.api.event.ChannelEvent] to the provided
 * [EventBus]. The [stop] method performs graceful shutdown and resource cleanup.
 *
 * @see ChannelBrand
 * @see Channel
 * @see EventBus
 * @see me.soknight.easydesk.channel.api.model.Conversation
 */
interface ChannelProvider {

    /** The brand describing this provider's platform. */
    val brand: ChannelBrand

    /** All registered channels (connections) for this provider. */
    val channels: List<Channel>

    /** Schema describing the configuration fields accepted by this provider. */
    val configSchema: ConfigSchema

    /**
     * Starts the provider: connects to the platform and begins publishing
     * events to the [eventBus].
     *
     * Long-running work (polling loops, webhook listeners) should be launched
     * within the provided [scope] so that cancellation propagates correctly.
     *
     * @param scope coroutine scope for launching background jobs
     * @param eventBus the event bus to publish incoming events to
     */
    suspend fun start(scope: CoroutineScope, eventBus: EventBus)

    /**
     * Gracefully stops the provider: disconnects from the platform
     * and releases any held resources.
     */
    suspend fun stop()

}
