package me.soknight.easydesk.supervisor.api.binding

import me.soknight.easydesk.supervisor.api.SupervisorBrand
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Binds a ticket to a supervisor surface topic/thread.
 *
 * The [nativeId] is surface-specific (e.g., Telegram `forum_topic_id`). A
 * ticket has at most one binding per [brand].
 *
 * @see SupervisorBrand
 */
data class TicketSupervisorBinding(
    /** Unique identifier of the ticket. */
    val ticketId: Long,
    /** The supervisor surface type. */
    val brand: SupervisorBrand,
    /** Platform-native identifier (e.g., Telegram `forum_topic_id`). */
    val nativeId: String,
    /** When the binding was created. */
    val createdAt: Instant = Clock.System.now(),
)
