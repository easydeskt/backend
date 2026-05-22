@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.service.agents.domain.Agent

@Serializable
data class AgentResponse(
    @SerialName("added_by_agent_id") val addedByAgentId: String?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("display_name") val displayName: String,
    val id: String,
    @SerialName("is_active") val isActive: Boolean,
    val role: String,
    @SerialName("telegram_username") val telegramUsername: String?,
    @SerialName("updated_at") val updatedAt: Instant,
)

fun Agent.toResponse(telegramUsername: String? = null) = AgentResponse(
    addedByAgentId = addedByAgentId?.toString(),
    createdAt = createdAt,
    displayName = displayName,
    id = identifier.toString(),
    isActive = isActive,
    role = role.key,
    telegramUsername = telegramUsername,
    updatedAt = updatedAt,
)
