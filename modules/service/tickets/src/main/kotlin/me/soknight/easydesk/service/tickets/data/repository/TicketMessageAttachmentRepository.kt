package me.soknight.easydesk.service.tickets.data.repository

import io.ktor.http.ContentType
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageAttachment

/**
 * Persistence contract for [TicketMessageAttachment] management.
 *
 * Stores metadata-only records for inbound message attachments.
 * No file bytes are persisted — only platform-reported properties
 * and remote references (e.g., Telegram `file_id`) via [attributes].
 *
 * All methods run inside a suspended transaction.
 */
interface TicketMessageAttachmentRepository {

    /**
     * Creates and persists a new attachment metadata record for a ticket message.
     *
     * @param messageId id of the owning message
     * @param kind media type category
     * @param fileName original filename reported by the platform
     * @param contentType MIME type reported by the platform
     * @param fileSize byte count reported by the platform, or `null` if unknown
     * @param channelBrand stable platform identifier (e.g., `"telegram"`, `"email"`)
     * @param attributes extensible platform-specific metadata; defaults to an empty object
     * @return the persisted [TicketMessageAttachment] with its generated identifier
     */
    suspend fun create(
        messageId: Long,
        kind: Attachment.Kind,
        fileName: String,
        contentType: ContentType,
        fileSize: Long?,
        channelBrand: String,
        attributes: JsonObject = JsonObject(emptyMap()),
    ): TicketMessageAttachment

    /**
     * Returns all attachment metadata records for the given message, in insertion order.
     */
    suspend fun findByMessage(messageId: Long): List<TicketMessageAttachment>

}
