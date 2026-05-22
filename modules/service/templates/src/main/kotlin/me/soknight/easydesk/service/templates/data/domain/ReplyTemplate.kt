@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.templates.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.supervisor.api.model.ReplyTemplate as SupervisorReplyTemplate

/**
 * A pre-written reply template for quick reuse by agents.
 *
 * A template carries an optional [content] body with placeholders and up to
 * [ReplyTemplateAttachment.MAX_PER_TEMPLATE] [attachments]. At least one of (`content`,
 * `attachments`) must be present — invariant enforced at the repository layer.
 *
 * Templates are channel-agnostic: at send time they are materialised into
 * `Conversation.send {...}` calls via `channel:api`, with placeholders rendered from the
 * outgoing context (see `docs/agent-interfaces.md §5`).
 *
 * @param identifier internal auto-generated identifier
 * @param humanName unique human-readable label shown in the template picker
 * @param content optional body with placeholders, or `null` for attachment-only templates
 * @param attachments ordered list of attachments (ordering preserved across reads)
 * @param createdBy id of the agent who created the template
 * @param createdAt timestamp of creation
 * @param updatedAt timestamp of the last edit
 *
 * @see ReplyTemplateAttachment
 * @see me.soknight.easydesk.service.templates.data.dto.ReplyTemplateAttachmentDto
 */
data class ReplyTemplate(
    override val identifier: Long,
    override val humanName: String,
    override val content: String?,
    override val attachments: List<ReplyTemplateAttachment>,
    val createdBy: Uuid,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = createdAt,
) : SupervisorReplyTemplate
