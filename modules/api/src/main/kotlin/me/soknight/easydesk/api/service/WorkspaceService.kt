@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.service

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.soknight.easydesk.api.config.WorkspaceConfig
import me.soknight.easydesk.api.response.TicketsCounters
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

    private var metricsCache: WorkspaceMetrics? = null
    private var metricsCacheExpiry: Instant = Instant.DISTANT_PAST
    private val metricsMutex = Mutex()
    private val processStartedAt: Instant = ProcessHandle.current().info().startInstant()
        .map { it.toKotlinInstant() }
        .orElseGet { Clock.System.now() }

    val startedAt: Long get() = processStartedAt.toEpochMilliseconds()
    val workspaceName: String get() = config.name
    val version: String = WorkspaceService::class.java
        .getResourceAsStream("/version.properties")
        ?.use { java.util.Properties().apply { load(it) }.getProperty("version") }
        ?: WorkspaceService::class.java.`package`?.implementationVersion
        ?: "unknown"

    suspend fun getMetrics(): WorkspaceMetrics = metricsMutex.withLock {
        val now = Clock.System.now()
        metricsCache?.takeIf { now < metricsCacheExpiry } ?: fetchMetrics().also {
            metricsCache = it
            metricsCacheExpiry = now + 1.minutes
        }
    }

    suspend fun getSuperadminId(): Uuid? = agentRepository.findSuperadmin()?.identifier

    private suspend fun fetchMetrics(): WorkspaceMetrics = coroutineScope {
        val closedDeferred = async { ticketRepository.countByStatuses(Status.CLOSED) }
        val inProgressDeferred = async { ticketRepository.countByStatuses(Status.IN_PROGRESS) }
        val mergedDeferred = async { ticketRepository.countByStatuses(Status.MERGED) }
        val openDeferred = async { ticketRepository.countByStatuses(Status.OPEN) }
        val resolvedDeferred = async { ticketRepository.countByStatuses(Status.RESOLVED) }
        val avgResponseDeferred = async { ticketRepository.avgFirstResponseTimeMinutes() }
        WorkspaceMetrics(
            avgResponseTime = avgResponseDeferred.await() ?: 0.0,
            ticketsCounters = TicketsCounters(
                closed = closedDeferred.await(),
                inProgress = inProgressDeferred.await(),
                merged = mergedDeferred.await(),
                open = openDeferred.await(),
                resolved = resolvedDeferred.await(),
            ),
        )
    }

}
