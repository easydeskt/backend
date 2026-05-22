@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.supervisor.api.model.Ticket.Priority
import me.soknight.easydesk.supervisor.api.model.Ticket.Status
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import kotlin.uuid.ExperimentalUuidApi

internal object TicketsTable : LongIdTable("tickets") {

    val assignedAgentId = uuid("assigned_agent_id").nullable()
    val assignedAt = timestamp("assigned_at").nullable()
    val attributes = jsonb<JsonObject>("attributes", Json)
    val closedAt = timestamp("closed_at").nullable()
    val conversationId = long("conversation_id")
    val createdAt = timestamp("created_at")
    val mergedAt = timestamp("merged_at").nullable()
    val mergedIntoTicketId = long("merged_into_ticket_id").nullable()
    val priority = enumerationByName<Priority>("priority", 16)
    val readUpToMessageId = long("read_up_to_message_id").nullable()
    val resolvedAt = timestamp("resolved_at").nullable()
    val status = enumerationByName<Status>("status", 16)
    val updatedAt = timestamp("updated_at")

}
