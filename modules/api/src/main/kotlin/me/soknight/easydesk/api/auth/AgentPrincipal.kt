package me.soknight.easydesk.api.auth

import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.supervisor.api.model.Agent.Role

data class AgentPrincipal(
    val agent: Agent,
    val telegramUsername: String? = null,
) {
    val isAdmin: Boolean get() = agent.role == Role.ADMIN
}
