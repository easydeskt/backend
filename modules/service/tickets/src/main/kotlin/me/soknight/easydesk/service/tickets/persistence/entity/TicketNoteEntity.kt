@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.entity

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.service.tickets.data.domain.TicketNote
import me.soknight.easydesk.service.tickets.persistence.table.TicketNotesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class TicketNoteEntity(id: EntityID<Long>) : LongEntity(id) {

    var authorAgentId : Uuid by TicketNotesTable.authorAgentId
    var createdAt     : Instant by TicketNotesTable.createdAt
    var text          : String by TicketNotesTable.text
    var ticketId      : Long by TicketNotesTable.ticketId
    var updatedAt     : Instant by TicketNotesTable.updatedAt

    fun toDomain() = TicketNote(
        authorAgentId = authorAgentId,
        createdAt = createdAt,
        identifier = id.value,
        text = text,
        ticketId = ticketId,
        updatedAt = updatedAt,
    )

    companion object : LongEntityClass<TicketNoteEntity>(TicketNotesTable)

}
