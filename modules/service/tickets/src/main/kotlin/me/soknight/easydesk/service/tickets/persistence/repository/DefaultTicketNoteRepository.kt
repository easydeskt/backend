@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.repository

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.tickets.data.domain.TicketNote
import me.soknight.easydesk.service.tickets.data.repository.TicketNoteRepository
import me.soknight.easydesk.service.tickets.persistence.entity.TicketNoteEntity
import me.soknight.easydesk.service.tickets.persistence.table.TicketNotesTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Single
internal class DefaultTicketNoteRepository : TicketNoteRepository {

    override suspend fun create(ticketId: Long, text: String, authorAgentId: Uuid): TicketNote =
        suspendTransaction {
            val now = Clock.System.now()
            TicketNoteEntity.new {
                this.authorAgentId = authorAgentId
                this.createdAt = now
                this.text = text
                this.ticketId = ticketId
                this.updatedAt = now
            }
        }.toDomain()

    override suspend fun delete(id: Long): Boolean =
        suspendTransaction {
            val entity = TicketNoteEntity.findById(id) ?: return@suspendTransaction false
            entity.delete()
            true
        }

    override suspend fun findByTicket(ticketId: Long): List<TicketNote> =
        suspendTransaction {
            TicketNoteEntity
                .find { TicketNotesTable.ticketId eq ticketId }
                .orderBy(TicketNotesTable.createdAt to SortOrder.DESC)
                .map(TicketNoteEntity::toDomain)
        }

    override suspend fun findById(id: Long): TicketNote? =
        suspendTransaction { TicketNoteEntity.findById(id) }?.toDomain()

    override suspend fun update(id: Long, text: String): TicketNote? =
        suspendTransaction {
            val entity = TicketNoteEntity.findById(id) ?: return@suspendTransaction null
            entity.text = text
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

}
