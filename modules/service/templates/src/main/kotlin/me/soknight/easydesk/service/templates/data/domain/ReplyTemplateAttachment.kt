package me.soknight.easydesk.service.templates.data.domain

import io.ktor.http.*
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.storage.data.domain.Attachment
import me.soknight.easydesk.supervisor.api.model.ReplyTemplate

/**
 * Association between a [ReplyTemplate] and a stored attachment.
 *
 * Reply template attachments are channel-agnostic: the same attachment can be sent
 * to any channel. Fields are stored directly rather than delegated to
 * [Attachment][me.soknight.easydesk.service.storage.data.domain.Attachment] because
 * templates are not bound to a specific channel at storage time.
 *
 * @param templateId id of the owning template
 * @param attachmentId identifier assigned by the storage layer
 * @param contentType MIME type
 * @param fileName original filename from the platform
 * @param fileSize byte count, or `null` if not reported by the platform
 * @param attachmentKind media type category
 * @param storagePath opaque path for retrieving the file bytes
 * @param attributes extensible platform-specific metadata
 * @param createdAt timestamp of recording
 *
 * @see ReplyTemplate
 */
data class ReplyTemplateAttachment(
    override val templateId: Long,
    override val attachmentId: Long,
    override val contentType: ContentType,
    override val fileName: String,
    override val fileSize: Long?,
    val attachmentKind: Attachment.Kind,
    val storagePath: String,
    val attributes: JsonObject,
    val createdAt: Instant,
) : ReplyTemplate.Attachment {

    override val kind: String get() = attachmentKind.name

    companion object {

        /**
         * Maximum number of attachments a single [ReplyTemplate] may carry.
         *
         * Matches the Telegram media-group limit; enforced at the service layer rather than
         * as a database CHECK constraint, because the cap is a platform concern, not a
         * storage invariant.
         */
        const val MAX_PER_TEMPLATE = 10

    }

}
