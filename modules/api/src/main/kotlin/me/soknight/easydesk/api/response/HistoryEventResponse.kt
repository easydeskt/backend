@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.audit.data.domain.AuditEvent

@Serializable
data class HistoryEventResponse(
    @SerialName("agent_id") val agentId: String?,
    @SerialName("agent_name") val agentName: String?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("event_type") val eventType: String,
    val id: Long,
    val payload: JsonObject,
)

fun AuditEvent.toResponse(agentName: String?) = HistoryEventResponse(
    agentId = agentId?.toString(),
    agentName = agentName,
    createdAt = createdAt,
    eventType = eventType,
    id = identifier,
    payload = payload,
)
