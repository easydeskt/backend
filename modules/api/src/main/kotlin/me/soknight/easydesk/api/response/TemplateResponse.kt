package me.soknight.easydesk.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.supervisor.api.model.ReplyTemplate

@Serializable
data class TemplateResponse(
    val content: String?,
    @SerialName("has_attachments") val hasAttachments: Boolean,
    @SerialName("human_name") val humanName: String,
    val id: Long,
)

fun ReplyTemplate.toResponse() = TemplateResponse(
    content = content,
    hasAttachments = attachments.isNotEmpty(),
    humanName = humanName,
    id = identifier,
)
