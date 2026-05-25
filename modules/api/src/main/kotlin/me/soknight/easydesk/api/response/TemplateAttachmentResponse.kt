package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
import me.soknight.easydesk.supervisor.api.model.ReplyTemplate

@Serializable
data class TemplateAttachmentResponse(
    @SerialName("attachment_id") val attachmentId: Long,
    @SerialName("content_type") val contentType: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: Long?,
    val attributes: JsonObject,
    val kind: String,
    @SerialName("template_id") val templateId: Long,
)

fun ReplyTemplate.Attachment.toAttachmentResponse(): TemplateAttachmentResponse {
    val rta = this as? ReplyTemplateAttachment ?: error("Expected ReplyTemplateAttachment")
    return TemplateAttachmentResponse(
        attachmentId = attachmentId,
        attributes = rta.attributes,
        contentType = contentType.toString(),
        createdAt = rta.createdAt,
        fileName = fileName,
        fileSize = fileSize,
        kind = kind,
        templateId = templateId,
    )
}
