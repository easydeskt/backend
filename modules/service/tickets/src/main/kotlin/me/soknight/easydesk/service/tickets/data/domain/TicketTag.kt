package me.soknight.easydesk.service.tickets.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import me.soknight.easydesk.supervisor.api.model.TicketTag as SupervisorTicketTag

/**
 * A label that can be applied to [Ticket]s for categorization and filtering.
 *
 * Tag names are unique system-wide. [color] is an optional RGBA color packed
 * as a signed 32-bit integer (bytes: R G B A from most-significant to least-significant)
 * for visual differentiation in the UI.
 *
 * @param identifier internal auto-generated identifier
 * @param humanName unique label (max 32 characters)
 * @param color optional RGBA int32 color for UI display
 * @param createdAt timestamp of creation
 */
data class TicketTag(
    override val identifier: Long,
    override val humanName: String,
    override val color: Int?,
    override val createdAt: Instant = Clock.System.now(),
) : SupervisorTicketTag
