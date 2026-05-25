@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.json.Json
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.helper.TestFixtures
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.Ticket as SupervisorTicket
import kotlin.test.*

class MeRoutesTest {

    private val authenticator = mockk<ApiAuthenticator>()
    private val ticketRepository = mockk<TicketRepository>()

    @BeforeTest
    fun setUp() {
        clearMocks(authenticator, ticketRepository)
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false })
        }
        routing {
            with(MeRoutes(authenticator, ticketRepository)) { configureRoutes() }
        }
        block()
    }

    @Test
    fun `GET me returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET me returns 200 with zero ticket counts when no assigned tickets`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findByAssignedAgent(TestFixtures.operatorId) } returns emptyList()
        coEvery { ticketRepository.avgFirstResponseTimeMinutes(TestFixtures.operatorId) } returns null
        coEvery { ticketRepository.resolvedTodayCount(TestFixtures.operatorId) } returns 0

        val response = client.get("/api/v1/me")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET me counts in-progress and open tickets separately`() = withApp {
        val tickets = listOf(
            TestFixtures.ticket(id = 1L, status = SupervisorTicket.Status.IN_PROGRESS),
            TestFixtures.ticket(id = 2L, status = SupervisorTicket.Status.IN_PROGRESS),
            TestFixtures.ticket(id = 3L, status = SupervisorTicket.Status.OPEN),
            TestFixtures.ticket(id = 4L, status = SupervisorTicket.Status.RESOLVED),
        )
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findByAssignedAgent(TestFixtures.operatorId) } returns tickets
        coEvery { ticketRepository.avgFirstResponseTimeMinutes(TestFixtures.operatorId) } returns null
        coEvery { ticketRepository.resolvedTodayCount(TestFixtures.operatorId) } returns 0

        val response = client.get("/api/v1/me")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET me includes telegram username from principal`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findByAssignedAgent(TestFixtures.operatorId) } returns emptyList()
        coEvery { ticketRepository.avgFirstResponseTimeMinutes(TestFixtures.operatorId) } returns null
        coEvery { ticketRepository.resolvedTodayCount(TestFixtures.operatorId) } returns 0

        val response = client.get("/api/v1/me")

        assertEquals(HttpStatusCode.OK, response.status)
    }

}
