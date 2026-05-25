package me.soknight.easydesk.service.tickets.persistence.repository

import io.ktor.http.ContentType
import kotlin.time.Clock
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageAttachment
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.persistence.entity.TicketMessageAttachmentEntity
import me.soknight.easydesk.service.tickets.persistence.table.TicketMessageAttachmentsTable
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Single
internal class DefaultTicketMessageAttachmentRepository : TicketMessageAttachmentRepository {

    override suspend fun create(
        messageId: Long,
        kind: Attachment.Kind,
        fileName: String,
        contentType: ContentType,
        fileSize: Long?,
        channelBrand: String,
        attributes: JsonObject,
    ): TicketMessageAttachment =
        suspendTransaction {
            TicketMessageAttachmentEntity.new {
                this.attributes   = attributes
                this.channelBrand = channelBrand
                this.contentType  = contentType.toString()
                this.createdAt    = Clock.System.now()
                this.fileName     = fileName
                this.fileSize     = fileSize
                this.kind         = kind
                this.messageId    = messageId
            }
        }.toDomain()

    override suspend fun findByMessage(messageId: Long): List<TicketMessageAttachment> =
        suspendTransaction {
            TicketMessageAttachmentEntity
                .find { TicketMessageAttachmentsTable.messageId eq messageId }
                .map(TicketMessageAttachmentEntity::toDomain)
        }

}
