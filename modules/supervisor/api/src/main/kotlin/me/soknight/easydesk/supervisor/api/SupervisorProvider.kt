package me.soknight.easydesk.supervisor.api

import kotlinx.coroutines.CoroutineScope
import me.soknight.easydesk.core.event.EventBus

/**
 * A concrete supervisor surface implementation.
 *
 * Each supported management interface (Telegram supergroup, CLI admin, etc.)
 * provides its own [SupervisorProvider], registered via Koin `@Single`, that
 * manages the surface lifecycle and reacts to system events.
 *
 * ### Lifecycle
 *
 * The provider is stateless until [start] is called. Once started, it connects
 * to the management surface, subscribes to the [EventBus], and projects state
 * changes (ticket events, audit events) to the surface. The [stop] method
 * performs graceful shutdown and resource cleanup.
 *
 * @see SupervisorBrand
 * @see me.soknight.easydesk.core.event.EventBus
 */
interface SupervisorProvider {

    /** The brand describing this provider's surface type. */
    val brand: SupervisorBrand

    /**
     * Starts the provider: connects to the management surface and begins
     * consuming events from the [eventBus].
     *
     * Long-running work (polling, WebSocket listeners, etc.) should be
     * launched within the provided [scope] so cancellation propagates.
     *
     * @param scope coroutine scope for launching background jobs
     * @param eventBus the event bus to subscribe to for state change events
     */
    suspend fun start(scope: CoroutineScope, eventBus: EventBus)

    /**
     * Gracefully stops the provider: disconnects from the surface and
     * releases any held resources.
     */
    suspend fun stop()

}
