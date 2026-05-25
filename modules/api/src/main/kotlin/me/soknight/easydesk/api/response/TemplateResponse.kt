package me.soknight.easydesk.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.supervisor.api.model.ReplyTemplate

@Serializable
data class TemplateResponse(
    val attachments: List<TemplateAttachmentResponse>,
    val content: String?,
    @SerialName("human_name") val humanName: String,
    val id: Long,
)

fun ReplyTemplate.toResponse() = TemplateResponse(
    attachments = attachments.map { it.toAttachmentResponse() },
    content = content,
    humanName = humanName,
    id = identifier,
)
