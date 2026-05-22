package me.soknight.easydesk.core.event

import kotlinx.coroutines.flow.SharedFlow

/**
 * Central event bus for all application events.
 *
 * Channel providers publish platform events here, and service modules
 * subscribe to receive them. Decouples event producers (channels) from
 * consumers (tickets, notifications, audit, etc.).
 *
 * Events are not persisted — if no collector is active, events are lost.
 * Slow collectors are suspended (backpressure) until buffer capacity
 * is available.
 *
 * ```kotlin
 * // publishing (inside a channel provider)
 * eventBus.publish(event)
 *
 * // subscribing (inside a service)
 * eventBus.events.collect { event -> handleEvent(event) }
 * ```
 *
 * @see Event
 */
interface EventBus {

    /** A shared flow of all application events. */
    val events: SharedFlow<Event>

    /** Publishes an event to all active subscribers. */
    suspend fun publish(event: Event)

}
