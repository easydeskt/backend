@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.service

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.soknight.easydesk.api.config.WorkspaceConfig
import me.soknight.easydesk.api.response.WorkspaceMetrics
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.Ticket.Status
import org.koin.core.annotation.Single

@Single
class WorkspaceService(
    private val agentRepository: AgentRepository,
    private val config: WorkspaceConfig,
    private val ticketRepository: TicketRepository,
) {

    private val metricsMutex = Mutex()
    private var metricsCache: WorkspaceMetrics? = null
    private var metricsCacheExpiry: Instant = Instant.DISTANT_PAST

    val workspaceName: String get() = config.name

    suspend fun getMetrics(): WorkspaceMetrics = metricsMutex.withLock {
        val now = Clock.System.now()
        metricsCache?.takeIf { now < metricsCacheExpiry } ?: fetchMetrics().also {
            metricsCache = it
            metricsCacheExpiry = now + 1.minutes
        }
    }

    suspend fun getSuperadminId(): Uuid? = agentRepository.findSuperadmin()?.identifier

    private suspend fun fetchMetrics(): WorkspaceMetrics = coroutineScope {
        val openDeferred = async { ticketRepository.countByStatuses(Status.OPEN, Status.IN_PROGRESS) }
        val inProgressDeferred = async { ticketRepository.countByStatuses(Status.IN_PROGRESS) }
        val resolvedDeferred = async { ticketRepository.countByStatuses(Status.RESOLVED) }
        val avgResponseDeferred = async { ticketRepository.avgFirstResponseTimeMinutes() }
        WorkspaceMetrics(
            avgResponseTime = avgResponseDeferred.await() ?: 0.0,
            openTickets = openDeferred.await(),
            ticketsInProgress = inProgressDeferred.await(),
            ticketsResolved = resolvedDeferred.await(),
        )
    }

}
