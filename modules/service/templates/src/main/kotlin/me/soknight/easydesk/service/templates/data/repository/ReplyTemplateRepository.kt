@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.templates.data.repository

import me.soknight.easydesk.service.templates.data.domain.ReplyTemplate
import me.soknight.easydesk.service.templates.data.dto.ReplyTemplateAttachmentDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persistence contract for [ReplyTemplate] management.
 *
 * All methods run inside a suspended transaction.
 *
 * **Update semantics are PUT-like**: [update] takes the full new state of the template,
 * including the complete attachment list. This matches the immutability of attachments
 * per row — editing the attachment set means full replacement.
 *
 * **Invariant** (enforced by every mutating method): `content` is non-blank or
 * `attachments` is non-empty (or both). The list size must not exceed
 * [ReplyTemplateAttachment.MAX_PER_TEMPLATE].
 */
interface ReplyTemplateRepository {

    /**
     * Creates and persists a new reply template with its attachments.
     *
     * [name] must be unique; a duplicate insert will throw a database constraint violation.
     * Attachment order follows the index in [attachments] and is preserved across reads.
     *
     * @param name unique label for the template
     * @param content optional reply text, or `null` for an attachment-only template
     * @param createdBy id of the creating agent
     * @param attachments ordered list of attachments (length 0..[me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment.MAX_PER_TEMPLATE])
     * @return the persisted [ReplyTemplate] with its generated [id][ReplyTemplate.identifier] and
     *   fully-populated [attachments][ReplyTemplate.attachments]
     * @throws IllegalArgumentException if both `content` is blank and `attachments` is
     *   empty, or if [attachments] exceeds [me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment.MAX_PER_TEMPLATE]
     */
    suspend fun create(
        name: String,
        content: String?,
        createdBy: Uuid,
        attachments: List<ReplyTemplateAttachmentDto>,
    ): ReplyTemplate

    /** Returns all reply templates, with their attachments eager-loaded. */
    suspend fun findAll(): List<ReplyTemplate>

    /** Returns the template with the given [id], with attachments, or `null` if not found. */
    suspend fun findById(id: Long): ReplyTemplate?

    /** Returns the template with the given [name], with attachments, or `null` if not found. */
    suspend fun findByName(name: String): ReplyTemplate?

    /**
     * Replaces the full state of the template with [id]. Returns `null` if no template
     * with [id] exists.
     *
     * The previous attachment set is deleted and replaced with [attachments]; order
     * follows the index in the input list and is preserved across reads.
     *
     * @param id id of the template to update
     * @param name new unique label
     * @param content new body, or `null` for an attachment-only template
     * @param attachments full new ordered list of attachments
     * @throws IllegalArgumentException if both `content` is blank and `attachments` is
     *   empty, or if [attachments] exceeds [ReplyTemplateAttachment.MAX_PER_TEMPLATE]
     */
    suspend fun update(
        id: Long,
        name: String,
        content: String?,
        attachments: List<ReplyTemplateAttachmentDto>,
    ): ReplyTemplate?

    /**
     * Deletes the template with the given [id]. Cascades to its attachments at the
     * database level; cleanup of the corresponding `service:storage` blobs is the
     * responsibility of the caller (or a dedicated GC job).
     *
     * Returns `true` if the template existed and was deleted, `false` otherwise.
     */
    suspend fun delete(id: Long): Boolean

}
