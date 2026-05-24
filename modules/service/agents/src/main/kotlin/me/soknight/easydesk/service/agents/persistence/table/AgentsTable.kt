@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.agents.persistence.table

import me.soknight.easydesk.core.persistence.pgEnum
import me.soknight.easydesk.supervisor.api.model.Agent.Role
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

internal object AgentsTable : UuidTable("agents") {

    val addedByAgentId = uuid("added_by_agent_id").nullable()
    val createdAt = timestamp("created_at")
    val displayName = varchar("display_name", 128)
    val isActive = bool("is_active")
    val role = pgEnum<Role>("role", "agent_role")
    val updatedAt = timestamp("updated_at")

}
