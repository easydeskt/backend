@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.supervisor.api.model.ChannelIdentity
import me.soknight.easydesk.supervisor.api.model.IdentityNote

@Serializable
data class IdentityNoteResponse(
    @SerialName("author_agent_id") val authorAgentId: String,
    @SerialName("created_at") val createdAt: Instant,
    val id: Long,
    @SerialName("identity_id") val identityId: Long,
    val text: String,
    @SerialName("updated_at") val updatedAt: Instant,
)

@Serializable
data class IdentityResponse(
    val brand: String,
    @SerialName("display_name") val displayName: String?,
    val id: Long,
    @SerialName("native_id") val nativeId: String,
)

fun ChannelIdentity.toResponse() = IdentityResponse(
    brand = channelBrand.identifier,
    displayName = displayName,
    id = identifier,
    nativeId = nativeId,
)

fun IdentityNote.toResponse() = IdentityNoteResponse(
    authorAgentId = authorAgentId.toString(),
    createdAt = createdAt,
    id = identifier,
    identityId = identityId,
    text = text,
    updatedAt = updatedAt,
)
