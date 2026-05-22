@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.supervisor.api.model.TicketMessage

@Serializable
data class TicketMessageResponse(
    val id: Long,
    @SerialName("in_reply_to_native_id") val inReplyToNativeId: String?,
    @SerialName("native_id") val nativeId: String,
    @SerialName("plain_text") val plainText: String?,
    @SerialName("platform_timestamp") val platformTimestamp: Instant,
    @SerialName("sender_agent_id") val senderAgentId: String?,
    @SerialName("sender_identity_id") val senderIdentityId: Long?,
    @SerialName("sender_kind") val senderKind: String,
    @SerialName("ticket_id") val ticketId: Long,
)

fun TicketMessage.toResponse() = TicketMessageResponse(
    id = identifier,
    inReplyToNativeId = inReplyToNativeId,
    nativeId = nativeId,
    plainText = plainText,
    platformTimestamp = platformTimestamp,
    senderAgentId = senderAgentId?.toString(),
    senderIdentityId = senderIdentityId,
    senderKind = senderKind.key,
    ticketId = ticketId,
)
