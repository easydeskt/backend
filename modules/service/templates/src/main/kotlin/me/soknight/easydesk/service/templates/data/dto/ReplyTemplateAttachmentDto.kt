package me.soknight.easydesk.service.templates.data.dto

import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Attachment.Kind

/**
 * Input value used when creating or replacing the attachment set of a [me.soknight.easydesk.service.templates.data.domain.ReplyTemplate].
 *
 * Carries everything a [me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment] needs except
 * [attachmentId][me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment.attachmentId] and
 * [createdAt][me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment.createdAt],
 * which are assigned by the repository.
 *
 * @see me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
 * @see me.soknight.easydesk.service.templates.data.repository.ReplyTemplateRepository
 */
data class ReplyTemplateAttachmentDto(
    val kind: Kind,
    val storagePath: String,
    val fileName: String,
    val contentType: ContentType,
    val fileSize: Long? = null,
    val attributes: JsonObject = JsonObject(emptyMap()),
    val attachmentId: Long? = null,
)