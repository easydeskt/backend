@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.templates.persistence.repository

import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplate
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
import me.soknight.easydesk.service.templates.data.dto.ReplyTemplateAttachmentDto
import me.soknight.easydesk.service.templates.data.repository.ReplyTemplateRepository
import me.soknight.easydesk.service.templates.persistence.entity.ReplyTemplateAttachmentEntity
import me.soknight.easydesk.service.templates.persistence.entity.ReplyTemplateEntity
import me.soknight.easydesk.service.templates.persistence.table.ReplyTemplateAttachmentsTable
import me.soknight.easydesk.service.templates.persistence.table.ReplyTemplatesTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Single
internal class DefaultReplyTemplateRepository : ReplyTemplateRepository {

    override suspend fun create(
        name: String,
        content: String?,
        createdBy: Uuid,
        attachments: List<ReplyTemplateAttachmentDto>,
    ): ReplyTemplate =
        suspendTransaction {
            requireValidPayload(content, attachments)
            val now = Clock.System.now()

            val template = ReplyTemplateEntity.new {
                this.content = content
                this.createdAt = now
                this.createdBy = createdBy
                this.name = name
                this.updatedAt = now
            }

            template.toDomain(persistAttachments(template.id.value, attachments, now))
        }

    override suspend fun findAll(): List<ReplyTemplate> =
        suspendTransaction {
            val templates = ReplyTemplateEntity.all().toList()
            val attachmentsByTemplate = loadAttachmentsFor(templates.map { it.id.value })
            templates.map { it.toDomain(attachmentsByTemplate[it.id.value].orEmpty()) }
        }

    override suspend fun findById(id: Long): ReplyTemplate? =
        suspendTransaction {
            val template = ReplyTemplateEntity.findById(id) ?: return@suspendTransaction null
            template.toDomain(loadAttachments(id))
        }

    override suspend fun findByName(name: String): ReplyTemplate? =
        suspendTransaction {
            val template = ReplyTemplateEntity
                .find { ReplyTemplatesTable.name eq name }
                .singleOrNull()
                ?: return@suspendTransaction null

            template.toDomain(loadAttachments(template.id.value))
        }

    override suspend fun update(
        id: Long,
        name: String,
        content: String?,
        attachments: List<ReplyTemplateAttachmentDto>,
    ): ReplyTemplate? =
        suspendTransaction {
            requireValidPayload(content, attachments)
            val template = ReplyTemplateEntity.findById(id) ?: return@suspendTransaction null
            val now = Clock.System.now()

            template.content = content
            template.name = name
            template.updatedAt = now

            ReplyTemplateAttachmentEntity
                .find { ReplyTemplateAttachmentsTable.templateId eq id }
                .toList()
                .forEach { it.delete() }

            template.toDomain(persistAttachments(id, attachments, now))
        }

    override suspend fun delete(id: Long): Boolean =
        suspendTransaction {
            val entity = ReplyTemplateEntity.findById(id) ?: return@suspendTransaction false
            entity.delete()
            true
        }

    private fun persistAttachments(
        templateId: Long,
        attachments: List<ReplyTemplateAttachmentDto>,
        now: Instant,
    ): List<ReplyTemplateAttachment> = attachments.mapIndexed { index, src ->
        ReplyTemplateAttachmentEntity.new {
            this.attributes = src.attributes
            this.contentType = src.contentType.toString()
            this.createdAt = now
            this.fileName = src.fileName
            this.fileSize = src.fileSize
            this.kind = src.kind
            this.position = index
            this.storagePath = src.storagePath
            this.templateId = templateId
        }.toDomain()
    }

    private fun loadAttachments(templateId: Long): List<ReplyTemplateAttachment> =
        with(ReplyTemplateAttachmentsTable) {
            ReplyTemplateAttachmentEntity
                .find { this.templateId eq templateId }
                .orderBy(position to SortOrder.ASC)
                .map { it.toDomain() }
        }

    private fun loadAttachmentsFor(templateIds: List<Long>): Map<Long, List<ReplyTemplateAttachment>> {
        if (templateIds.isEmpty()) return emptyMap()

        return with(ReplyTemplateAttachmentsTable) {
            ReplyTemplateAttachmentEntity
                .find { templateId inList templateIds }
                .orderBy(position to SortOrder.ASC)
                .groupBy { it.templateId }
                .mapValues { (_, list) -> list.map { it.toDomain() } }
        }
    }

    private fun requireValidPayload(content: String?, attachments: List<ReplyTemplateAttachmentDto>) {
        require(!content.isNullOrBlank() || attachments.isNotEmpty()) {
            "Reply template must have either content text or at least one attachment"
        }

        require(attachments.size <= ReplyTemplateAttachment.MAX_PER_TEMPLATE) {
            "Reply template cannot have more than ${ReplyTemplateAttachment.MAX_PER_TEMPLATE} attachments " +
                "(got ${attachments.size})"
        }
    }

}
