@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.audit.data.repository

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.audit.data.domain.AuditEvent
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persistence contract for [AuditEvent] management.
 *
 * All methods run inside a suspended transaction.
 * Events are append-only — no update or delete operations are exposed.
 */
interface AuditEventRepository {

    /**
     * Creates and persists a new audit event.
     *
     * @param ticketId id of the affected ticket, or `null` for system-level events
     * @param agentId id of the acting agent, or `null` for system-generated events
     * @param eventType opaque string key identifying the action (e.g. `"ticket.assigned"`)
     * @param payload structured metadata for the event; defaults to an empty object
     * @return the persisted [AuditEvent] with its generated [id][AuditEvent.identifier]
     */
    suspend fun create(
        eventType: String,
        ticketId: Long? = null,
        agentId: Uuid? = null,
        payload: JsonObject? = null,
    ): AuditEvent

    /**
     * Returns the audit event with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): AuditEvent?

    /**
     * Returns all audit events for the given ticket in ascending chronological order.
     *
     * @param ticketId id of the ticket whose history to retrieve
     */
    suspend fun findByTicket(ticketId: Long): List<AuditEvent>

    /**
     * Returns audit events of the given [eventType] in descending chronological order.
     *
     * @param eventType opaque string key to filter by
     * @param limit maximum number of events to return; `null` returns all
     */
    suspend fun findByType(eventType: String, limit: Int? = null): List<AuditEvent>

}
