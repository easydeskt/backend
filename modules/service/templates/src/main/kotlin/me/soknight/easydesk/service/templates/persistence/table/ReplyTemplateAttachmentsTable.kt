package me.soknight.easydesk.service.templates.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.persistence.pgEnum
import me.soknight.easydesk.service.storage.data.domain.Attachment.Kind
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

internal object ReplyTemplateAttachmentsTable : LongIdTable("reply_template_attachments") {

    val attributes = jsonb<JsonObject>("attributes", Json)
    val contentType = varchar("content_type", 128)
    val createdAt = timestamp("created_at")
    val fileName = varchar("file_name", 512)
    val fileSize = long("file_size").nullable()
    val kind = pgEnum<Kind>("kind", "attachment_kind")
    val position = integer("position").default(0)
    val storagePath = varchar("storage_path", 1024)
    val templateId = long("template_id")

}
