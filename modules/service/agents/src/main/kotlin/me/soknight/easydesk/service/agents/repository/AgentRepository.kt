@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.agents.repository

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.supervisor.api.SupervisorBrand
import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent

/**
 * Persistence contract for [Agent] management.
 *
 * All methods run inside a suspended transaction. Callers do not need to wrap calls
 * in a transaction themselves — each method is self-contained for MVP.
 */
interface AgentRepository {

    /**
     * Creates and persists a new agent.
     *
     * @param displayName human-readable name for the agent
     * @param role access level; defaults to [SupervisorAgent.Role.OPERATOR]
     * @param addedByAgentId id of the registering agent, or `null` for the bootstrap admin
     * @return the persisted [Agent] with its generated [id][Agent.identifier]
     */
    suspend fun create(
        displayName: String,
        role: SupervisorAgent.Role = SupervisorAgent.Role.OPERATOR,
        addedByAgentId: Uuid? = null,
    ): Agent

    /**
     * Returns all agents, optionally filtered to active ones only.
     *
     * @param activeOnly when `true` (default), excludes soft-deleted agents
     */
    suspend fun findAll(activeOnly: Boolean = true): List<Agent>

    /**
     * Returns a map of agent id to binding attributes for all agents bound to the given supervisor platform.
     * Agents without a binding on this platform are absent from the map.
     *
     * @param brand supervisor platform brand
     */
    suspend fun findAllBindingAttributes(brand: SupervisorBrand): Map<Uuid, JsonObject>

    /**
     * Returns binding attributes for the given agent on the given supervisor platform,
     * or `null` if no binding exists.
     *
     * @param agentId id of the agent
     * @param brand supervisor platform brand
     */
    suspend fun findBindingAttributes(agentId: Uuid, brand: SupervisorBrand): JsonObject?

    /**
     * Returns the agent with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Uuid): Agent?

    /**
     * Returns the agent bound to the given supervisor platform identity, or `null` if not found.
     *
     * @param brand supervisor platform brand
     * @param nativeId platform-specific user id (e.g. Telegram `user_id` as string)
     */
    suspend fun findBySupervisorBinding(brand: SupervisorBrand, nativeId: String): Agent?

    /**
     * Binds an agent to a supervisor platform identity.
     *
     * The pair `(brand, nativeId)` must be unique across all agents; a duplicate insert
     * will throw a database constraint violation.
     *
     * @param agentId id of the agent to bind
     * @param brand supervisor platform brand
     * @param nativeId platform-specific user id (e.g. Telegram `user_id` as string)
     */
    suspend fun linkSupervisor(agentId: Uuid, brand: SupervisorBrand, nativeId: String)

    /**
     * Merges [patch] into the existing binding attributes for the given agent on the given platform.
     * Keys present in [patch] overwrite existing keys; other keys are preserved.
     * No-op if no binding exists for the `(agentId, brand)` pair.
     *
     * @param agentId id of the agent
     * @param brand supervisor platform brand
     * @param patch attributes to merge in
     */
    suspend fun patchBindingAttributes(agentId: Uuid, brand: SupervisorBrand, patch: JsonObject)

    /**
     * Updates mutable fields of an existing agent.
     * Returns `null` if no agent with [id] exists.
     *
     * @param id id of the agent to update
     * @param displayName new human-readable name, or `null` to keep the current one
     * @param isActive new active state, or `null` to keep the current one
     * @param role new role, or `null` to keep the current one
     */
    suspend fun update(
        id: Uuid,
        displayName: String? = null,
        isActive: Boolean? = null,
        role: SupervisorAgent.Role? = null,
    ): Agent?

}
