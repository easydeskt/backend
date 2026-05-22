@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.supervisor.api.model.Ticket as SupervisorTicket

/**
 * The central entity of EasyDesk — a support request from a client.
 *
 * A ticket represents one thread of communication between a client (via [conversationId])
 * and the support team. Agents work inside a Telegram supergroup topic that corresponds
 * to one ticket.
 *
 * When [status] is [SupervisorTicket.Status.MERGED], [mergedIntoTicketId] must be non-null (enforced by DB CHECK).
 *
 * @param identifier internal auto-generated identifier
 * @param conversationId id of the conversation this ticket belongs to
 * @param status current lifecycle state
 * @param priority operational urgency level
 * @param assignedAgentId id of the currently assigned agent, or `null` if unassigned
 * @param mergedIntoTicketId id of the ticket this one was merged into; non-null iff [status] is [SupervisorTicket.Status.MERGED]
 * @param attributes extensible platform-specific metadata; also holds app-level fields such as `resolution_note`
 * @param assignedAt timestamp when the ticket was last assigned
 * @param closedAt timestamp when the ticket was closed, or `null` if not yet closed
 * @param mergedAt timestamp when the ticket was merged, or `null` if not merged
 * @param resolvedAt timestamp when the ticket was resolved, or `null` if not yet resolved
 * @param readUpToMessageId id of the last message that was marked as read, or `null` if none
 * @param createdAt timestamp of creation
 * @param updatedAt timestamp of last modification
 */
data class Ticket(
    override val identifier: Long,
    override val conversationId: Long,
    override val status: SupervisorTicket.Status,
    override val priority: SupervisorTicket.Priority,
    override val assignedAgentId: Uuid?,
    override val mergedIntoTicketId: Long?,
    val attributes: JsonObject,
    val assignedAt: Instant? = null,
    val closedAt: Instant? = null,
    val mergedAt: Instant? = null,
    val resolvedAt: Instant? = null,
    val readUpToMessageId: Long? = null,
    override val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = createdAt,
) : SupervisorTicket
