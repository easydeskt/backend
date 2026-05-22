@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.templates.persistence.entity

import me.soknight.easydesk.service.templates.data.domain.ReplyTemplate
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
import me.soknight.easydesk.service.templates.persistence.table.ReplyTemplatesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ReplyTemplateEntity(id: EntityID<Long>) : LongEntity(id) {

    var content     : String? by ReplyTemplatesTable.content
    var createdAt   : Instant by ReplyTemplatesTable.createdAt
    var createdBy   : Uuid by ReplyTemplatesTable.createdBy
    var name        : String by ReplyTemplatesTable.name
    var updatedAt   : Instant by ReplyTemplatesTable.updatedAt

    fun toDomain(attachments: List<ReplyTemplateAttachment>) = ReplyTemplate(
        attachments = attachments,
        content = content,
        createdAt = createdAt,
        createdBy = createdBy,
        identifier = id.value,
        humanName = name,
        updatedAt = updatedAt,
    )

    companion object : LongEntityClass<ReplyTemplateEntity>(ReplyTemplatesTable)

}
