@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.agents.domain

import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A helpdesk agent (operator or administrator) within EasyDesk.
 *
 * Agents work inside the Telegram supergroup with forum topics and have access to the Mini App.
 * [SupervisorAgent.Role.ADMIN] extends [SupervisorAgent.Role.OPERATOR] privileges — there is no separate permission storage.
 *
 * @param identifier internal auto-generated UUID; also serves as a stable public reference
 * @param displayName human-readable name shown in the UI and audit log
 * @param role access level; defaults to [SupervisorAgent.Role.OPERATOR]
 * @param isActive whether the agent can log in and act; soft-delete flag
 * @param addedByAgentId id of the agent who registered this agent, or `null` for the bootstrap admin
 * @param createdAt timestamp of registration
 * @param updatedAt timestamp of the last profile change
 */
data class Agent(
    override val identifier: Uuid,
    override val displayName: String,
    override val role: SupervisorAgent.Role = SupervisorAgent.Role.OPERATOR,
    override val isActive: Boolean = true,
    val addedByAgentId: Uuid? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = createdAt,
) : SupervisorAgent
