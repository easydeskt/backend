package me.soknight.easydesk.service.tickets.data.domain

import io.ktor.http.ContentType
import kotlin.time.Instant
import kotlinx.serialization.json.JsonElement
import me.soknight.easydesk.channel.api.model.Attachment

/**
 * Metadata record for a file attached to an inbound [TicketMessage].
 *
 * Stored in `ticket_message_attachments`. Contains only remote references
 * and platform-reported metadata — no file bytes and no local storage path.
 * Channel-specific identifiers (e.g., Telegram `file_id`) are stored in [attributes].
 *
 * @param identifier internal auto-generated identifier
 * @param messageId id of the owning message
 * @param kind media type category
 * @param fileName original filename reported by the platform
 * @param contentType MIME type reported by the platform
 * @param fileSize byte count reported by the platform, or `null` if unknown
 * @param channelBrand stable platform identifier (e.g., `"telegram"`, `"email"`)
 * @param attributes extensible platform-specific metadata (e.g., remote file references)
 * @param createdAt timestamp of local recording
 *
 * @see TicketMessage
 * @see Attachment
 */
data class TicketMessageAttachment(
    val attributes: Map<String, JsonElement>,
    val channelBrand: String,
    val contentType: ContentType,
    val createdAt: Instant,
    val fileName: String,
    val fileSize: Long?,
    val identifier: Long,
    val kind: Attachment.Kind,
    val messageId: Long,
)
