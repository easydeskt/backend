@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.agents.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

internal object AgentSupervisorBindingsTable : Table("agent_supervisor_bindings") {

    val agentId = uuid("agent_id")
    val attributes = jsonb<JsonObject>("attributes", Json)
    val createdAt = timestamp("created_at")
    val nativeId = varchar("native_id", 256)
    val supervisorBrand = varchar("supervisor_brand", 16)

    override val primaryKey = PrimaryKey(agentId, supervisorBrand)

}
