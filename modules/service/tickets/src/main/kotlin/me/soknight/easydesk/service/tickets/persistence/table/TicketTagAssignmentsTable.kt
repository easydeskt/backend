@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

internal object TicketTagAssignmentsTable : Table("ticket_tag_assignments") {

    val addedAt = timestamp("added_at")
    val addedByAgentId = uuid("added_by_agent_id")
    val sortOrder = integer("sort_order")
    val tagId = long("tag_id")
    val ticketId = long("ticket_id")

    override val primaryKey = PrimaryKey(ticketId, tagId)

}
