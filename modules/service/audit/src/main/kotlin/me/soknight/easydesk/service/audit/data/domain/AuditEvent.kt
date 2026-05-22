@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.audit.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.EMPTY_JSON_OBJECT
import me.soknight.easydesk.supervisor.api.model.AuditEvent as SupervisorAuditEvent

/**
 * An immutable record of a notable system or agent action.
 *
 * Audit events are append-only and are never modified after creation.
 * They form the history trail visible in the pinned topic of the supergroup.
 *
 * @param identifier internal auto-generated identifier
 * @param eventType opaque string key identifying the action (e.g. `"ticket.assigned"`)
 * @param ticketId id of the affected ticket, or `null` for system-level events
 * @param agentId id of the acting agent, or `null` for system-generated events
 * @param payload structured metadata attached to the event
 * @param createdAt timestamp of recording
 */
data class AuditEvent(
    override val identifier: Long,
    override val eventType: String,
    override val ticketId: Long? = null,
    override val agentId: Uuid? = null,
    override val payload: JsonObject = EMPTY_JSON_OBJECT,
    override val createdAt: Instant = Clock.System.now(),
) : SupervisorAuditEvent
