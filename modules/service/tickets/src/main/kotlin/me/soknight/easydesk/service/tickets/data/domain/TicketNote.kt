@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.supervisor.api.model.TicketNote as SupervisorTicketNote

/**
 * A free-text note attached to a [Ticket] by an agent.
 *
 * Notes serve as an internal knowledge base visible only to agents, not exposed to the client.
 *
 * @param identifier internal auto-generated identifier
 * @param ticketId id of the ticket this note is attached to
 * @param text note body
 * @param authorAgentId id of the agent who wrote the note
 * @param createdAt timestamp of creation
 * @param updatedAt timestamp of the last edit
 */
data class TicketNote(
    override val identifier: Long,
    override val ticketId: Long,
    override val text: String,
    override val authorAgentId: Uuid,
    override val createdAt: Instant = Clock.System.now(),
    override val updatedAt: Instant = createdAt,
) : SupervisorTicketNote
