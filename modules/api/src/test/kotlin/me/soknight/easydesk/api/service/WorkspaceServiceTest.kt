@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.service

import io.mockk.*
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.test.runTest
import me.soknight.easydesk.api.config.WorkspaceConfig
import me.soknight.easydesk.api.helper.TestFixtures
import me.soknight.easydesk.api.response.TicketsCounters
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.Ticket.Status

class WorkspaceServiceTest {

    private val agentRepository = mockk<AgentRepository>()
    private val config = mockk<WorkspaceConfig>()
    private val ticketRepository = mockk<TicketRepository>()

    private lateinit var service: WorkspaceService

    @BeforeTest
    fun setUp() {
        clearMocks(agentRepository, config, ticketRepository)
        every { config.name } returns "Test Workspace"
        service = WorkspaceService(agentRepository, config, ticketRepository)
    }

    // --- getMetrics ---

    @Test
    fun `getMetrics returns correct counters from repositories`() = runTest {
        coEvery { ticketRepository.countByStatuses(Status.CLOSED) } returns 1L
        coEvery { ticketRepository.countByStatuses(Status.IN_PROGRESS) } returns 2L
        coEvery { ticketRepository.countByStatuses(Status.MERGED) } returns 3L
        coEvery { ticketRepository.countByStatuses(Status.OPEN) } returns 4L
        coEvery { ticketRepository.countByStatuses(Status.RESOLVED) } returns 5L
        coEvery { ticketRepository.avgFirstResponseTimeMinutes() } returns 2.5

        val result = service.getMetrics()

        assertEquals(
            TicketsCounters(closed = 1L, inProgress = 2L, merged = 3L, open = 4L, resolved = 5L),
            result.ticketsCounters,
        )
        assertEquals(2.5, result.avgResponseTime)
    }

    @Test
    fun `getMetrics returns 0 avg response time when repository returns null`() = runTest {
        coEvery { ticketRepository.countByStatuses(any()) } returns 0L
        coEvery { ticketRepository.avgFirstResponseTimeMinutes() } returns null

        val result = service.getMetrics()

        assertEquals(0.0, result.avgResponseTime)
    }

    @Test
    fun `getMetrics caches result and does not re-fetch on second call`() = runTest {
        coEvery { ticketRepository.countByStatuses(any()) } returns 0L
        coEvery { ticketRepository.avgFirstResponseTimeMinutes() } returns 0.0

        service.getMetrics()
        service.getMetrics()

        coVerify(exactly = 1) { ticketRepository.countByStatuses(Status.OPEN) }
        coVerify(exactly = 1) { ticketRepository.avgFirstResponseTimeMinutes() }
    }

    // --- getSuperadminId ---

    @Test
    fun `getSuperadminId returns null when no superadmin exists`() = runTest {
        coEvery { agentRepository.findSuperadmin() } returns null

        assertNull(service.getSuperadminId())
    }

    @Test
    fun `getSuperadminId returns uuid when superadmin exists`() = runTest {
        coEvery { agentRepository.findSuperadmin() } returns TestFixtures.adminAgent

        assertEquals(TestFixtures.adminId, service.getSuperadminId())
    }

    // --- workspaceName ---

    @Test
    fun `workspaceName returns value from config`() {
        assertEquals("Test Workspace", service.workspaceName)
    }

}
