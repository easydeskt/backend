@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.audit.persistence.repository

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.EMPTY_JSON_OBJECT
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.audit.data.domain.AuditEvent
import me.soknight.easydesk.service.audit.data.repository.AuditEventRepository
import me.soknight.easydesk.service.audit.persistence.entity.AuditEventEntity
import me.soknight.easydesk.service.audit.persistence.table.AuditEventsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Single
internal class DefaultAuditEventRepository : AuditEventRepository {

    override suspend fun create(
        eventType: String,
        ticketId: Long?,
        agentId: Uuid?,
        payload: JsonObject?,
    ): AuditEvent = suspendTransaction {
        AuditEventEntity.new {
            this.agentId = agentId
            this.createdAt = Clock.System.now()
            this.eventType = eventType
            this.payload = payload ?: EMPTY_JSON_OBJECT
            this.ticketId = ticketId
        }
    }.toDomain()

    override suspend fun findByTicket(ticketId: Long): List<AuditEvent> =
        suspendTransaction {
            with(AuditEventsTable) {
                AuditEventEntity
                    .find { this.ticketId eq ticketId }
                    .orderBy(this.createdAt to SortOrder.ASC)
                    .map(AuditEventEntity::toDomain)
            }
        }

    override suspend fun findByType(eventType: String, limit: Int?): List<AuditEvent> =
        suspendTransaction {
            with(AuditEventsTable) {
                AuditEventEntity
                    .find { this.eventType eq eventType }
                    .orderBy(createdAt to SortOrder.DESC)
                    .let { if (limit != null) it.limit(limit) else it }
                    .map(AuditEventEntity::toDomain)
            }
        }

    override suspend fun findById(id: Long): AuditEvent? =
        suspendTransaction {
            AuditEventEntity.findById(id)
        }?.toDomain()

}
