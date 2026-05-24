@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.agents.persistence.repository

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.persistence.entity.AgentEntity
import me.soknight.easydesk.service.agents.persistence.table.AgentSupervisorBindingsTable
import me.soknight.easydesk.service.agents.persistence.table.AgentsTable
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.api.SupervisorBrand
import me.soknight.easydesk.supervisor.api.model.Agent.Role
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single
internal class DefaultAgentRepository : AgentRepository {

    private val logger = getLogger()

    override suspend fun create(displayName: String, role: Role, addedByAgentId: Uuid?): Agent =
        suspendTransaction {
            val now = Clock.System.now()
            AgentEntity.new {
                this.addedByAgentId = addedByAgentId
                this.createdAt = now
                this.displayName = displayName
                this.isActive = true
                this.role = role
                this.updatedAt = now
            }
        }.toDomain()

    override suspend fun findAllBindingAttributes(brand: SupervisorBrand): Map<Uuid, JsonObject> =
        suspendTransaction {
            with(AgentSupervisorBindingsTable) {
                selectAll()
                    .where { supervisorBrand eq brand.identifier }
                    .associate { it[agentId] to it[attributes] }
            }
        }

    override suspend fun findAll(activeOnly: Boolean): List<Agent> =
        suspendTransaction {
            if (activeOnly) {
                AgentEntity.find { AgentsTable.isActive eq true }
            } else {
                AgentEntity.all()
            }.map(AgentEntity::toDomain)
        }

    override suspend fun findAllWithNullAddedBy(): List<Agent> =
        suspendTransaction {
            AgentEntity.find { AgentsTable.addedByAgentId.isNull() }
                .map(AgentEntity::toDomain)
        }

    override suspend fun findBindingAttributes(agentId: Uuid, brand: SupervisorBrand): JsonObject? =
        suspendTransaction {
            with(AgentSupervisorBindingsTable) {
                selectAll()
                    .where { (this.agentId eq agentId) and (supervisorBrand eq brand.identifier) }
                    .singleOrNull()
                    ?.get(attributes)
            }
        }

    override suspend fun findById(id: Uuid): Agent? =
        suspendTransaction { AgentEntity.findById(id) }?.toDomain()

    override suspend fun findBySupervisorBinding(brand: SupervisorBrand, nativeId: String): Agent? =
        suspendTransaction {
            val agentId = with(AgentSupervisorBindingsTable) {
                selectAll()
                    .where { (supervisorBrand eq brand.identifier) and (this.nativeId eq nativeId) }
                    .singleOrNull()
                    ?.get(agentId)
                    ?: return@suspendTransaction null
            }

            AgentEntity.findById(agentId)
        }?.toDomain()

    override suspend fun findSuperadmin(): Agent? {
        val candidates = suspendTransaction {
            AgentEntity.find { AgentsTable.addedByAgentId.isNull() }.map(AgentEntity::toDomain)
        }
        if (candidates.size > 1) {
            logger.warn(
                "findSuperadmin: {} agents have null addedByAgentId, expected exactly one — returning null",
                candidates.size,
            )
            return null
        }
        return candidates.singleOrNull()
    }

    override suspend fun fixBootstrap(superadminId: Uuid, agentIdsToFix: List<Uuid>) {
        if (agentIdsToFix.isEmpty()) return
        suspendTransaction {
            for (id in agentIdsToFix) {
                AgentEntity.findById(id)?.let { it.addedByAgentId = superadminId }
            }
        }
    }

    override suspend fun linkSupervisor(agentId: Uuid, brand: SupervisorBrand, nativeId: String) {
        suspendTransaction {
            with(AgentSupervisorBindingsTable) {
                insert {
                    it[this.agentId] = agentId
                    it[this.attributes] = JsonObject(emptyMap())
                    it[this.createdAt] = Clock.System.now()
                    it[this.nativeId] = nativeId
                    it[this.supervisorBrand] = brand.identifier
                }
            }
        }
    }

    override suspend fun patchBindingAttributes(agentId: Uuid, brand: SupervisorBrand, patch: JsonObject) {
        suspendTransaction {
            with(AgentSupervisorBindingsTable) {
                val current = selectAll()
                    .where { (this.agentId eq agentId) and (supervisorBrand eq brand.identifier) }
                    .singleOrNull()
                    ?.get(attributes)
                    ?: return@suspendTransaction
                update({ (this.agentId eq agentId) and (supervisorBrand eq brand.identifier) }) {
                    it[attributes] = JsonObject(current.toMap() + patch.toMap())
                }
            }
        }
    }

    override suspend fun update(id: Uuid, displayName: String?, isActive: Boolean?, role: Role?): Agent? =
        suspendTransaction {
            val entity = AgentEntity.findById(id) ?: return@suspendTransaction null
            if (displayName != null) entity.displayName = displayName
            if (isActive != null) entity.isActive = isActive
            if (role != null) entity.role = role
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

}
