@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.audit.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import kotlin.uuid.ExperimentalUuidApi

internal object AuditEventsTable : LongIdTable("audit_events") {

    val agentId = uuid("agent_id").nullable()
    val createdAt = timestamp("created_at")
    val eventType = varchar("event_type", 64)
    val payload = jsonb<JsonObject>("payload", Json)
    val ticketId = long("ticket_id").nullable()

}
