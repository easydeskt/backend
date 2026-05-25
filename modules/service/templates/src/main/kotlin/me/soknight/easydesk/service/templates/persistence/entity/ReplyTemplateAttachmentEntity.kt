package me.soknight.easydesk.service.templates.persistence.entity

import io.ktor.http.*
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Attachment.Kind
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
import me.soknight.easydesk.service.templates.persistence.table.ReplyTemplateAttachmentsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class ReplyTemplateAttachmentEntity(id: EntityID<Long>) : LongEntity(id) {

    var attributes  : JsonObject by ReplyTemplateAttachmentsTable.attributes
    var contentType : String by ReplyTemplateAttachmentsTable.contentType
    var createdAt   : Instant by ReplyTemplateAttachmentsTable.createdAt
    var fileName    : String by ReplyTemplateAttachmentsTable.fileName
    var fileSize    : Long? by ReplyTemplateAttachmentsTable.fileSize
    var kind        : Kind by ReplyTemplateAttachmentsTable.kind
    var position    : Int by ReplyTemplateAttachmentsTable.position
    var storagePath : String by ReplyTemplateAttachmentsTable.storagePath
    var templateId  : Long by ReplyTemplateAttachmentsTable.templateId

    fun toDomain() = ReplyTemplateAttachment(
        attachmentId = id.value,
        attachmentKind = kind,
        attributes = attributes,
        contentType = ContentType.parse(contentType),
        createdAt = createdAt,
        fileName = fileName,
        fileSize = fileSize,
        storagePath = storagePath,
        templateId = templateId,
    )

    companion object : LongEntityClass<ReplyTemplateAttachmentEntity>(ReplyTemplateAttachmentsTable)

}
