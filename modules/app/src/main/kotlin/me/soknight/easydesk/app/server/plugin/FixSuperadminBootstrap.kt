@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.app.server.plugin

import io.ktor.server.application.*
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.runBlocking
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import org.koin.ktor.ext.getKoin

private val logger = getLogger()

fun Application.fixSuperadminBootstrap() {
    val koin = getKoin()
    val agentRepository = koin.get<AgentRepository>()
    val supervisorConfig = koin.get<TelegramSupervisorConfig>()

    runBlocking {
        val nullParentAgents = agentRepository.findAllWithNullAddedBy()
        if (nullParentAgents.size <= 1) return@runBlocking

        val superadmin = agentRepository.findBySupervisorBinding(
            TelegramSupervisorBrand,
            supervisorConfig.superadminId.toString(),
        )

        if (superadmin == null || superadmin.addedByAgentId != null) {
            logger.warn(
                "Superadmin bootstrap fix skipped: could not identify real superadmin among {} agents with null addedByAgentId",
                nullParentAgents.size,
            )
            return@runBlocking
        }

        val toFix = nullParentAgents.filter { it.identifier != superadmin.identifier }
        agentRepository.fixBootstrap(superadmin.identifier, toFix.map { it.identifier })
        logger.info(
            "Superadmin bootstrap fix applied: set addedByAgentId={} on {} agent(s)",
            superadmin.identifier,
            toFix.size,
        )
    }
}
