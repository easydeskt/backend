package me.soknight.easydesk.service.storage.persistence.entity

import io.ktor.http.*
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.model.Attachment as ChannelAttachment
import me.soknight.easydesk.service.storage.data.domain.Attachment
import me.soknight.easydesk.service.storage.data.domain.Attachment.Kind
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import me.soknight.easydesk.service.storage.persistence.table.AttachmentsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class AttachmentEntity(id: EntityID<Long>) : LongEntity(id) {

    var attributes  : JsonObject by AttachmentsTable.attributes
    var contentType : String by AttachmentsTable.contentType
    var createdAt   : Instant by AttachmentsTable.createdAt
    var fileName    : String by AttachmentsTable.fileName
    var fileSize    : Long? by AttachmentsTable.fileSize
    var kind        : Kind by AttachmentsTable.kind
    var messageId   : Long by AttachmentsTable.messageId
    var storagePath : String by AttachmentsTable.storagePath

    fun toDomain(channel: Channel, storageService: AttachmentStorageService): ChannelAttachment {
        val base = Attachment.Base(
            id.value, kind, fileName, ContentType.parse(contentType),
            fileSize, storagePath, attributes, createdAt, channel, storageService,
        )
        return when (kind) {
            Kind.AUDIO    -> Attachment.Audio(base)
            Kind.DOCUMENT -> Attachment.Document(base)
            Kind.PHOTO    -> Attachment.Photo(base)
            Kind.STICKER  -> Attachment.Document(base)
            Kind.VIDEO    -> Attachment.Video(base)
            Kind.VOICE    -> Attachment.Voice(base)
        }
    }

    companion object : LongEntityClass<AttachmentEntity>(AttachmentsTable)

}
