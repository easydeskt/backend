@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.repository

import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.tickets.data.domain.TicketTag
import me.soknight.easydesk.service.tickets.data.repository.TicketTagRepository
import me.soknight.easydesk.service.tickets.persistence.entity.TicketTagEntity
import me.soknight.easydesk.service.tickets.persistence.table.TicketTagAssignmentsTable
import me.soknight.easydesk.service.tickets.persistence.table.TicketTagsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Single
internal class DefaultTicketTagRepository : TicketTagRepository {

    override suspend fun addToTicket(ticketId: Long, tagId: Long, addedByAgentId: Uuid) {
        suspendTransaction {
            with(TicketTagAssignmentsTable) {
                val nextOrder = selectAll()
                    .where { this.ticketId eq ticketId }
                    .maxOfOrNull { it[sortOrder] }
                    ?.let { it + 1 } ?: 0

                insert {
                    it[this.addedAt] = Clock.System.now()
                    it[this.addedByAgentId] = addedByAgentId
                    it[this.sortOrder] = nextOrder
                    it[this.tagId] = tagId
                    it[this.ticketId] = ticketId
                }
            }
        }
    }

    override suspend fun create(name: String, color: Int?): TicketTag =
        suspendTransaction {
            TicketTagEntity.new {
                this.color = color
                this.createdAt = Clock.System.now()
                this.name = name
            }
        }.toDomain()

    override suspend fun delete(id: Long): Boolean =
        suspendTransaction {
            val entity = TicketTagEntity.findById(id) ?: return@suspendTransaction false
            entity.delete()
            true
        }

    override suspend fun findAll(): List<TicketTag> =
        suspendTransaction { TicketTagEntity.all().map(TicketTagEntity::toDomain) }

    override suspend fun findById(id: Long): TicketTag? =
        suspendTransaction { TicketTagEntity.findById(id) }?.toDomain()

    override suspend fun findByName(name: String): TicketTag? =
        suspendTransaction {
            TicketTagEntity.find { TicketTagsTable.name eq name }.singleOrNull()
        }?.toDomain()

    override suspend fun findByTicket(ticketId: Long): List<TicketTag> =
        suspendTransaction {
            val tagIds = with(TicketTagAssignmentsTable) {
                selectAll()
                    .where { this.ticketId eq ticketId }
                    .orderBy(sortOrder to SortOrder.ASC)
                    .map { it[tagId] }
            }

            if (tagIds.isEmpty())
                return@suspendTransaction emptyList()

            val entitiesById = TicketTagEntity.forIds(tagIds).associateBy { it.id.value }
            tagIds.mapNotNull { entitiesById[it] }.map(TicketTagEntity::toDomain)
        }

    override suspend fun findTicketIdsByTag(tagId: Long): List<Long> =
        suspendTransaction {
            with(TicketTagAssignmentsTable) {
                selectAll()
                    .where { this.tagId eq tagId }
                    .map { it[ticketId] }
            }
        }

    override suspend fun removeFromTicket(ticketId: Long, tagId: Long): Boolean =
        suspendTransaction {
            val deleted = with(TicketTagAssignmentsTable) {
                deleteWhere { (this.ticketId eq ticketId) and (this.tagId eq tagId) }
            }
            deleted > 0
        }

    override suspend fun setTags(ticketId: Long, orderedTagIds: List<Long>, byAgentId: Uuid) {
        suspendTransaction {
            with(TicketTagAssignmentsTable) {
                deleteWhere { this.ticketId eq ticketId }
                val now = Clock.System.now()
                orderedTagIds.forEachIndexed { index, id ->
                    insert {
                        it[addedAt] = now
                        it[addedByAgentId] = byAgentId
                        it[sortOrder] = index
                        it[tagId] = id
                        it[this.ticketId] = ticketId
                    }
                }
            }
        }
    }

    override suspend fun update(id: Long, name: String, color: Int?): TicketTag? =
        suspendTransaction {
            val entity = TicketTagEntity.findById(id) ?: return@suspendTransaction null
            entity.color = color
            entity.name = name
            entity
        }?.toDomain()

}
