package me.soknight.easydesk.service.tickets.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.core.persistence.pgEnum
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

internal object TicketMessageAttachmentsTable : LongIdTable("ticket_message_attachments") {

    val attributes   = jsonb<JsonObject>("attributes", Json)
    val channelBrand = varchar("channel_brand", 64)
    val contentType  = varchar("content_type", 128)
    val createdAt    = timestamp("created_at")
    val fileName     = varchar("file_name", 512)
    val fileSize     = long("file_size").nullable()
    val kind         = pgEnum<Attachment.Kind>("kind", "attachment_kind")
    val messageId    = long("message_id")

}
