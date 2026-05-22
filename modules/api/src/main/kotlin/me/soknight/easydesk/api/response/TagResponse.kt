package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.supervisor.api.model.TicketTag

@Serializable
data class TagResponse(
    val color: Int?,
    @SerialName("created_at") val createdAt: Instant,
    val id: Long,
    val name: String,
)

fun TicketTag.toResponse() = TagResponse(
    color = color,
    createdAt = createdAt,
    id = identifier,
    name = humanName,
)
