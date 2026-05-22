@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.channels.data.domain.Channel
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageStats

@Serializable
data class TicketClientResponse(
    val brand: String,
    @SerialName("display_name") val displayName: String?,
    @SerialName("identity_id") val identityId: Long,
    @SerialName("native_id") val nativeId: String,
    val url: String?,
)

@Serializable
data class TicketChannelResponse(
    val brand: String,
    @SerialName("display_name") val displayName: String,
    val id: Long,
)

@Serializable
data class TicketSummaryResponse(
    @SerialName("assigned_agent_id") val assignedAgentId: String?,
    @SerialName("assigned_at") val assignedAt: Instant?,
    @SerialName("attachment_count") val attachmentCount: Int,
    val attributes: JsonObject,
    val channel: TicketChannelResponse,
    val client: TicketClientResponse,
    @SerialName("closed_at") val closedAt: Instant?,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("created_at") val createdAt: Instant,
    val id: Long,
    @SerialName("last_message_at") val lastMessageAt: Instant?,
    @SerialName("merged_at") val mergedAt: Instant?,
    @SerialName("merged_into_ticket_id") val mergedIntoTicketId: Long?,
    @SerialName("message_preview") val messagePreview: String?,
    val priority: String,
    @SerialName("resolved_at") val resolvedAt: Instant?,
    val status: String,
    val tags: List<TagResponse>,
    @SerialName("topic_url") val topicUrl: String?,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("updated_at") val updatedAt: Instant,
)

@Serializable
data class TicketDetailResponse(
    @SerialName("assigned_agent_id") val assignedAgentId: String?,
    @SerialName("assigned_at") val assignedAt: Instant?,
    @SerialName("attachment_count") val attachmentCount: Int,
    val attributes: JsonObject,
    val channel: TicketChannelResponse,
    val client: TicketClientResponse,
    @SerialName("closed_at") val closedAt: Instant?,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("created_at") val createdAt: Instant,
    val id: Long,
    @SerialName("last_message_at") val lastMessageAt: Instant?,
    @SerialName("merged_at") val mergedAt: Instant?,
    @SerialName("merged_into_ticket_id") val mergedIntoTicketId: Long?,
    @SerialName("message_preview") val messagePreview: String?,
    val notes: List<NoteResponse>,
    val priority: String,
    @SerialName("resolved_at") val resolvedAt: Instant?,
    val status: String,
    val tags: List<TagResponse>,
    @SerialName("topic_url") val topicUrl: String?,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("updated_at") val updatedAt: Instant,
)

fun ChannelIdentity.toClientResponse() = TicketClientResponse(
    brand = channelBrand.identifier,
    displayName = displayName,
    identityId = identifier,
    nativeId = nativeId,
    url = buildClientUrl(),
)

private fun ChannelIdentity.buildClientUrl(): String? = when (channelBrand.identifier) {
    "mail" -> "mailto:$nativeId"
    "vk"   -> "https://vk.com/id$nativeId"
    else   -> null  // "tg": username not stored in identity; null until Telegram enrichment is added
}

fun Channel.toChannelResponse() = TicketChannelResponse(
    brand = brand,
    displayName = displayName,
    id = id,
)

fun Ticket.toSummaryResponse(
    channel: Channel,
    identity: ChannelIdentity,
    stats: TicketMessageStats,
    topicUrl: String?,
    tags: List<TagResponse>,
) = TicketSummaryResponse(
    assignedAgentId = assignedAgentId?.toString(),
    assignedAt = assignedAt,
    attachmentCount = stats.attachmentCount,
    attributes = attributes,
    channel = channel.toChannelResponse(),
    client = identity.toClientResponse(),
    closedAt = closedAt,
    conversationId = conversationId,
    createdAt = createdAt,
    id = identifier,
    lastMessageAt = stats.lastMessageAt,
    mergedAt = mergedAt,
    mergedIntoTicketId = mergedIntoTicketId,
    messagePreview = stats.previewText,
    priority = priority.key,
    resolvedAt = resolvedAt,
    status = status.key,
    tags = tags,
    topicUrl = topicUrl,
    unreadCount = stats.unreadCount,
    updatedAt = updatedAt,
)

fun Ticket.toDetailResponse(
    channel: Channel,
    identity: ChannelIdentity,
    stats: TicketMessageStats,
    topicUrl: String?,
    tags: List<TagResponse>,
    notes: List<NoteResponse>,
) = TicketDetailResponse(
    assignedAgentId = assignedAgentId?.toString(),
    assignedAt = assignedAt,
    attachmentCount = stats.attachmentCount,
    attributes = attributes,
    channel = channel.toChannelResponse(),
    client = identity.toClientResponse(),
    closedAt = closedAt,
    conversationId = conversationId,
    createdAt = createdAt,
    id = identifier,
    lastMessageAt = stats.lastMessageAt,
    mergedAt = mergedAt,
    mergedIntoTicketId = mergedIntoTicketId,
    messagePreview = stats.previewText,
    notes = notes,
    priority = priority.key,
    resolvedAt = resolvedAt,
    status = status.key,
    tags = tags,
    topicUrl = topicUrl,
    unreadCount = stats.unreadCount,
    updatedAt = updatedAt,
)
