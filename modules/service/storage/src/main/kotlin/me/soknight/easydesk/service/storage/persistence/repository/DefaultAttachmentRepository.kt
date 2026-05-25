package me.soknight.easydesk.service.storage.persistence.repository

import kotlin.time.Clock
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.model.Attachment as ChannelAttachment
import me.soknight.easydesk.channel.api.model.Attachment.Kind
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.storage.data.domain.Attachment
import me.soknight.easydesk.service.storage.data.repository.AttachmentRepository
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import me.soknight.easydesk.service.storage.persistence.entity.AttachmentEntity
import me.soknight.easydesk.service.storage.persistence.table.AttachmentsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single

@Single
internal class DefaultAttachmentRepository(
    private val storageService: AttachmentStorageService,
) : AttachmentRepository {

    override suspend fun create(
        messageId: Long,
        kind: Kind,
        fileName: String,
        contentType: String,
        fileSize: Long?,
        storagePath: String,
        channel: Channel,
        attributes: JsonObject,
    ): ChannelAttachment =
        suspendTransaction {
            AttachmentEntity.new {
                this.attributes = attributes
                this.contentType = contentType
                this.createdAt = Clock.System.now()
                this.fileName = fileName
                this.fileSize = fileSize
                this.kind = kind
                this.messageId = messageId
                this.storagePath = storagePath
            }
        }.toDomain(channel, storageService)

    override suspend fun findById(id: Long, channel: Channel): ChannelAttachment? =
        suspendTransaction { AttachmentEntity.findById(id) }?.toDomain(channel, storageService)

    override suspend fun findByMessage(messageId: Long, channel: Channel): Attachments =
        suspendTransaction {
            AttachmentEntity
                .find { AttachmentsTable.messageId eq messageId }
                .map { it.toDomain(channel, storageService) }
        }

    override suspend fun countByMessageIds(messageIds: Collection<Long>): Int =
        if (messageIds.isEmpty()) 0
        else suspendTransaction {
            AttachmentsTable.selectAll()
                .where { AttachmentsTable.messageId inList messageIds }
                .count()
                .toInt()
        }

}
