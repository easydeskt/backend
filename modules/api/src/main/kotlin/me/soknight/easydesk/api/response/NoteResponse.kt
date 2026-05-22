@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.service.channels.data.domain.IdentityNote
import me.soknight.easydesk.service.tickets.data.domain.TicketNote

@Serializable
data class NoteResponse(
    @SerialName("author_agent_id") val authorAgentId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("created_at") val createdAt: Instant,
    val id: Long,
    val scope: String,
    val text: String,
    @SerialName("updated_at") val updatedAt: Instant,
)

fun IdentityNote.toNoteResponse(authorName: String) = NoteResponse(
    authorAgentId = authorAgentId.toString(),
    authorName = authorName,
    createdAt = createdAt,
    id = identifier,
    scope = "client",
    text = text,
    updatedAt = updatedAt,
)

fun TicketNote.toNoteResponse(authorName: String) = NoteResponse(
    authorAgentId = authorAgentId.toString(),
    authorName = authorName,
    createdAt = createdAt,
    id = identifier,
    scope = "ticket",
    text = text,
    updatedAt = updatedAt,
)
