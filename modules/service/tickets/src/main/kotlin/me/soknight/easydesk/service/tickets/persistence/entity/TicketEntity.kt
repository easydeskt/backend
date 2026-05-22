@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.entity

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.service.tickets.persistence.table.TicketsTable
import me.soknight.easydesk.supervisor.api.model.Ticket.Priority
import me.soknight.easydesk.supervisor.api.model.Ticket.Status
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class TicketEntity(id: EntityID<Long>) : LongEntity(id) {

    var assignedAgentId    : Uuid? by TicketsTable.assignedAgentId
    var assignedAt         : Instant? by TicketsTable.assignedAt
    var attributes         : JsonObject by TicketsTable.attributes
    var closedAt           : Instant? by TicketsTable.closedAt
    var conversationId     : Long by TicketsTable.conversationId
    var createdAt          : Instant by TicketsTable.createdAt
    var mergedAt           : Instant? by TicketsTable.mergedAt
    var mergedIntoTicketId : Long? by TicketsTable.mergedIntoTicketId
    var priority           : Priority by TicketsTable.priority
    var readUpToMessageId  : Long? by TicketsTable.readUpToMessageId
    var resolvedAt         : Instant? by TicketsTable.resolvedAt
    var status             : Status by TicketsTable.status
    var updatedAt          : Instant by TicketsTable.updatedAt

    fun toDomain() = Ticket(
        assignedAgentId = assignedAgentId,
        assignedAt = assignedAt,
        attributes = attributes,
        closedAt = closedAt,
        conversationId = conversationId,
        createdAt = createdAt,
        identifier = id.value,
        mergedAt = mergedAt,
        mergedIntoTicketId = mergedIntoTicketId,
        priority = priority,
        readUpToMessageId = readUpToMessageId,
        resolvedAt = resolvedAt,
        status = status,
        updatedAt = updatedAt,
    )

    companion object : LongEntityClass<TicketEntity>(TicketsTable)

}
