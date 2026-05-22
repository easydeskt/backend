@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.audit.persistence.entity

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.audit.data.domain.AuditEvent
import me.soknight.easydesk.service.audit.persistence.table.AuditEventsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AuditEventEntity(id: EntityID<Long>) : LongEntity(id) {

    var agentId     : Uuid? by AuditEventsTable.agentId
    var createdAt   : Instant by AuditEventsTable.createdAt
    var eventType   : String by AuditEventsTable.eventType
    var payload     : JsonObject by AuditEventsTable.payload
    var ticketId    : Long? by AuditEventsTable.ticketId

    fun toDomain() = AuditEvent(
        agentId = agentId,
        createdAt = createdAt,
        eventType = eventType,
        identifier = id.value,
        payload = payload,
        ticketId = ticketId,
    )

    companion object : LongEntityClass<AuditEventEntity>(AuditEventsTable)

}
