@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Read-only view of a ticket note (agent-written annotation on a specific ticket)
 * for supervisor surfaces.
 */
interface TicketNote {

    /** Id of the agent who wrote the note. */
    val authorAgentId: Uuid

    /** Timestamp of creation. */
    val createdAt: Instant

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Note body; visible only to agents, not exposed to the client. */
    val text: String

    /** Id of the ticket this note is attached to. */
    val ticketId: Long

    /** Timestamp of the last edit. */
    val updatedAt: Instant

}
