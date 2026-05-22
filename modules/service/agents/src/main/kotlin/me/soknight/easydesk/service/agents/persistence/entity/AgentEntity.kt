@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.agents.persistence.entity

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.persistence.table.AgentsTable
import me.soknight.easydesk.supervisor.api.model.Agent.Role
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass

internal class AgentEntity(id: EntityID<Uuid>) : UuidEntity(id) {

    var addedByAgentId  : Uuid?      by AgentsTable.addedByAgentId
    var createdAt       : Instant    by AgentsTable.createdAt
    var displayName     : String     by AgentsTable.displayName
    var isActive        : Boolean    by AgentsTable.isActive
    var role            : Role       by AgentsTable.role
    var updatedAt       : Instant    by AgentsTable.updatedAt

    fun toDomain() = Agent(
        addedByAgentId = addedByAgentId,
        createdAt = createdAt,
        displayName = displayName,
        identifier = id.value,
        isActive = isActive,
        role = role,
        updatedAt = updatedAt,
    )

    companion object : UuidEntityClass<AgentEntity>(AgentsTable)

}
