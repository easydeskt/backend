@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.model

import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Read-only view of an audit event for supervisor surfaces.
 */
interface AuditEvent {

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Id of the acting agent, or `null` for system-generated events. */
    val agentId: Uuid?

    /** Opaque string key identifying the action (e.g. `"ticket.assigned"`). */
    val eventType: String

    /** Structured metadata attached to the event. */
    val payload: JsonObject

    /** Id of the affected ticket, or `null` for system-level events. */
    val ticketId: Long?

    /** Timestamp of recording. */
    val createdAt: Instant

}
