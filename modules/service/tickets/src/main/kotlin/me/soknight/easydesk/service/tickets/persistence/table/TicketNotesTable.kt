@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

internal object TicketNotesTable : LongIdTable("ticket_notes") {

    val authorAgentId = uuid("author_agent_id")
    val createdAt = timestamp("created_at")
    val text = text("text")
    val ticketId = long("ticket_id")
    val updatedAt = timestamp("updated_at")

}
