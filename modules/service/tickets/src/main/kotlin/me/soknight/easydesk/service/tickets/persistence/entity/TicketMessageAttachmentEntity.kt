package me.soknight.easydesk.service.tickets.persistence.entity

import io.ktor.http.ContentType
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageAttachment
import me.soknight.easydesk.service.tickets.persistence.table.TicketMessageAttachmentsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class TicketMessageAttachmentEntity(id: EntityID<Long>) : LongEntity(id) {

    var attributes   : JsonObject          by TicketMessageAttachmentsTable.attributes
    var channelBrand : String              by TicketMessageAttachmentsTable.channelBrand
    var contentType  : String              by TicketMessageAttachmentsTable.contentType
    var createdAt    : Instant             by TicketMessageAttachmentsTable.createdAt
    var fileName     : String              by TicketMessageAttachmentsTable.fileName
    var fileSize     : Long?               by TicketMessageAttachmentsTable.fileSize
    var kind         : Attachment.Kind     by TicketMessageAttachmentsTable.kind
    var messageId    : Long                by TicketMessageAttachmentsTable.messageId

    fun toDomain() = TicketMessageAttachment(
        attributes   = attributes,
        channelBrand = channelBrand,
        contentType  = ContentType.parse(contentType),
        createdAt    = createdAt,
        fileName     = fileName,
        fileSize     = fileSize,
        identifier   = id.value,
        kind         = kind,
        messageId    = messageId,
    )

    companion object : LongEntityClass<TicketMessageAttachmentEntity>(TicketMessageAttachmentsTable)

}
