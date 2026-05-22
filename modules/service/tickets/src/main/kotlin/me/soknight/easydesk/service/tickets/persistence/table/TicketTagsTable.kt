package me.soknight.easydesk.service.tickets.persistence.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object TicketTagsTable : LongIdTable("ticket_tags") {

    val color = integer("color").nullable()
    val createdAt = timestamp("created_at")
    val name = varchar("name", 32)

}
