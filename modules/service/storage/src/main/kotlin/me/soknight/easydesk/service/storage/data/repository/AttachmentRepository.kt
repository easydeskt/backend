package me.soknight.easydesk.service.storage.data.repository

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.model.Attachment as ChannelAttachment
import me.soknight.easydesk.channel.api.model.Attachment.Kind
import me.soknight.easydesk.service.storage.data.domain.Attachment

/**
 * Persistence contract for [Attachment] management.
 *
 * All methods run inside a suspended transaction.
 */
interface AttachmentRepository {

    /**
     * Creates and persists a new attachment linked to a message.
     *
     * Media-specific metadata (duration, dimensions, performer, title) should be stored
     * as keys in [attributes] and will be exposed via type-safe getters on the typed subclass.
     *
     * @param messageId id of the owning message
     * @param kind media type category
     * @param fileName original filename from the platform
     * @param contentType MIME type
     * @param fileSize byte count, or `null` if not reported by the platform
     * @param storagePath opaque path assigned by `service:storage`
     * @param channel the channel this attachment belongs to
     * @param attributes extensible platform-specific metadata; defaults to an empty object
     * @return the persisted [Attachment] with its generated identifier
     */
    suspend fun create(
        messageId: Long,
        kind: Kind,
        fileName: String,
        contentType: String,
        fileSize: Long?,
        storagePath: String,
        channel: Channel,
        attributes: JsonObject = JsonObject(emptyMap()),
    ): ChannelAttachment

    /**
     * Returns the attachment with the given [id], or `null` if not found.
     *
     * @param id the attachment identifier
     * @param channel the channel this attachment belongs to
     */
    suspend fun findById(id: Long, channel: Channel): ChannelAttachment?

    /**
     * Returns all attachments for the given message.
     *
     * @param messageId id of the owning message
     * @param channel the channel the message belongs to
     */
    suspend fun findByMessage(messageId: Long, channel: Channel): Attachments

    /**
     * Returns the total number of attachments linked to any of the given message ids.
     *
     * Returns `0` when [messageIds] is empty.
     *
     * @param messageIds ids of the messages to count attachments for
     */
    suspend fun countByMessageIds(messageIds: Collection<Long>): Int

}
