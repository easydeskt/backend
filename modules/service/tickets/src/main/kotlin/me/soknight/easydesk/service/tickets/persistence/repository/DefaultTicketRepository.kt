@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.repository

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.service.tickets.persistence.entity.TicketEntity
import me.soknight.easydesk.service.tickets.persistence.table.TicketMessagesTable
import me.soknight.easydesk.service.tickets.persistence.table.TicketSupervisorBindingsTable
import me.soknight.easydesk.service.tickets.persistence.table.TicketTagAssignmentsTable
import me.soknight.easydesk.service.tickets.persistence.table.TicketsTable
import me.soknight.easydesk.supervisor.api.SupervisorBrand
import me.soknight.easydesk.supervisor.api.model.Ticket.Priority
import me.soknight.easydesk.supervisor.api.model.Ticket.Status
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single
internal class DefaultTicketRepository : TicketRepository {

    override suspend fun assign(id: Long, agentId: Uuid?): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.assignedAgentId = agentId
            entity.assignedAt = if (agentId != null) Clock.System.now() else null
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

    override suspend fun avgFirstResponseTimeMinutes(): Double? =
        suspendTransaction {
            exec("""
                SELECT AVG(EXTRACT(EPOCH FROM (m.first_at - t.created_at)) / 60.0)
                FROM tickets t
                JOIN (
                    SELECT ticket_id, MIN(platform_timestamp) AS first_at
                    FROM ticket_messages
                    WHERE sender_kind = 'AGENT' -- enumerationByName stores enum name, not ActorKind.key
                    GROUP BY ticket_id
                ) m ON m.ticket_id = t.id
            """.trimIndent()) { rs ->
                if (rs.next()) rs.getDouble(1).takeIf { !rs.wasNull() } else null
            }
        }

    override suspend fun close(id: Long): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            val now = Clock.System.now()
            entity.closedAt = now
            entity.status = Status.CLOSED
            entity.updatedAt = now
            entity
        }?.toDomain()

    override suspend fun countByStatuses(vararg statuses: Status): Long {
        if (statuses.isEmpty()) return 0L
        return suspendTransaction {
            TicketsTable
                .selectAll()
                .where { TicketsTable.status inList statuses.toList() }
                .count()
        }
    }

    override suspend fun create(conversationId: Long, priority: Priority): Ticket =
        suspendTransaction {
            val now = Clock.System.now()
            TicketEntity.new {
                this.assignedAgentId = null
                this.assignedAt = null
                this.attributes = JsonObject(emptyMap())
                this.closedAt = null
                this.conversationId = conversationId
                this.createdAt = now
                this.mergedAt = null
                this.mergedIntoTicketId = null
                this.priority = priority
                this.readUpToMessageId = null
                this.resolvedAt = null
                this.status = Status.OPEN
                this.updatedAt = now
            }
        }.toDomain()

    override suspend fun findAll(): List<Ticket> =
        suspendTransaction {
            TicketEntity.all()
                .orderBy(TicketsTable.createdAt to SortOrder.DESC)
                .map(TicketEntity::toDomain)
        }

    override suspend fun findByAssignedAgent(agentId: Uuid): List<Ticket> =
        suspendTransaction {
            TicketEntity.find { TicketsTable.assignedAgentId eq agentId }.map(TicketEntity::toDomain)
        }

    override suspend fun findByConversation(conversationId: Long): List<Ticket> =
        suspendTransaction {
            TicketEntity.find { TicketsTable.conversationId eq conversationId }.map(TicketEntity::toDomain)
        }

    override suspend fun findById(id: Long): Ticket? =
        suspendTransaction { TicketEntity.findById(id) }?.toDomain()

    override suspend fun findByStatus(status: Status): List<Ticket> =
        suspendTransaction {
            TicketEntity.find { TicketsTable.status eq status }.map(TicketEntity::toDomain)
        }

    override suspend fun findOpenByConversation(conversationId: Long): Ticket? =
        suspendTransaction {
            TicketEntity
                .find {
                    (TicketsTable.conversationId eq conversationId) and
                    (TicketsTable.status inList listOf(Status.OPEN, Status.IN_PROGRESS))
                }
                .orderBy(TicketsTable.createdAt to SortOrder.DESC)
                .firstOrNull()
        }?.toDomain()

    override suspend fun findSupervisorBinding(ticketId: Long, brand: SupervisorBrand): String? =
        suspendTransaction {
            with(TicketSupervisorBindingsTable) {
                selectAll()
                    .where { (this.ticketId eq ticketId) and (supervisorBrand eq brand.identifier) }
                    .singleOrNull()
                    ?.get(nativeId)
            }
        }

    override suspend fun findTicketBySupervisorBinding(brand: SupervisorBrand, nativeId: String): Long? =
        suspendTransaction {
            with(TicketSupervisorBindingsTable) {
                selectAll()
                    .where { (supervisorBrand eq brand.identifier) and (this.nativeId eq nativeId) }
                    .singleOrNull()
                    ?.get(ticketId)
            }
        }

    override suspend fun free(id: Long): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.assignedAgentId = null
            entity.assignedAt = null
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

    override suspend fun linkSupervisor(ticketId: Long, brand: SupervisorBrand, nativeId: String) {
        suspendTransaction {
            with(TicketSupervisorBindingsTable) {
                insert {
                    it[this.createdAt] = Clock.System.now()
                    it[this.nativeId] = nativeId
                    it[this.supervisorBrand] = brand.identifier
                    it[this.ticketId] = ticketId
                }
            }
        }
    }

    override suspend fun merge(sourceId: Long, targetId: Long, byAgentId: Uuid): Pair<Ticket, Ticket>? =
        suspendTransaction {
            val source = TicketEntity.findById(sourceId) ?: return@suspendTransaction null
            val target = TicketEntity.findById(targetId) ?: return@suspendTransaction null

            if (source.conversationId != target.conversationId) return@suspendTransaction null

            val now = Clock.System.now()

            // Re-parent all messages from source to target
            with(TicketMessagesTable) {
                update({ ticketId eq sourceId }) { it[ticketId] = targetId }
            }

            // Merge tags: add source-only tags to target with sort_order continuation
            with(TicketTagAssignmentsTable) {
                val targetTagIds = selectAll().where { ticketId eq targetId }.map { it[tagId] }.toSet()
                val maxSortOrder = selectAll().where { ticketId eq targetId }
                    .maxOfOrNull { it[sortOrder] } ?: -1

                selectAll().where { ticketId eq sourceId }
                    .filter { it[tagId] !in targetTagIds }
                    .forEachIndexed { index, row ->
                        insert {
                            it[addedAt] = now
                            it[addedByAgentId] = byAgentId
                            it[sortOrder] = maxSortOrder + 1 + index
                            it[tagId] = row[tagId]
                            it[ticketId] = targetId
                        }
                    }
            }

            // Finalize source: mark as merged
            source.mergedAt = now
            source.mergedIntoTicketId = targetId
            source.status = Status.MERGED
            source.updatedAt = now

            // Finalize target: reopen and assign to initiator
            target.assignedAgentId = byAgentId
            target.assignedAt = now
            target.status = Status.OPEN
            target.updatedAt = now

            source.toDomain() to target.toDomain()
        }

    override suspend fun reopen(id: Long, byAgentId: Uuid): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.status = Status.OPEN
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

    override suspend fun resolve(id: Long): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            val now = Clock.System.now()
            entity.resolvedAt = now
            entity.status = Status.RESOLVED
            entity.updatedAt = now
            entity
        }?.toDomain()

    override suspend fun updateAttributes(id: Long, patch: JsonObject, replace: Boolean): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.attributes = when {
                replace -> JsonObject(patch.filterValues { it !is JsonNull })
                else    -> JsonObject((entity.attributes + patch).filterValues { it !is JsonNull })
            }
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

    override suspend fun updatePriority(id: Long, priority: Priority): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.priority = priority
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

    override suspend fun updateReadMarker(id: Long, messageId: Long): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.readUpToMessageId = messageId
            entity
        }?.toDomain()

    override suspend fun updateStatus(id: Long, status: Status, mergedIntoTicketId: Long?): Ticket? =
        suspendTransaction {
            val entity = TicketEntity.findById(id) ?: return@suspendTransaction null
            entity.status = status
            entity.mergedIntoTicketId = mergedIntoTicketId
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

}
