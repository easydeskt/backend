package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
import me.soknight.easydesk.supervisor.api.model.ReplyTemplate

@Serializable
data class TemplateAttachmentResponse(
    @SerialName("attachment_id") val attachmentId: Long,
    @SerialName("content_type") val contentType: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: Long?,
    val kind: String,
    @SerialName("template_id") val templateId: Long,
)

fun ReplyTemplate.Attachment.toAttachmentResponse() = TemplateAttachmentResponse(
    attachmentId = attachmentId,
    contentType = contentType.toString(),
    createdAt = (this as? ReplyTemplateAttachment)?.createdAt
        ?: error("Expected ReplyTemplateAttachment"),
    fileName = fileName,
    fileSize = fileSize,
    kind = kind,
    templateId = templateId,
)
