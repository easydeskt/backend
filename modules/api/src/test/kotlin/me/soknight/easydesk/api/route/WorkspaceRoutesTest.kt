@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.Json
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.helper.TestFixtures
import me.soknight.easydesk.api.response.TicketsCounters
import me.soknight.easydesk.api.response.WorkspaceMetrics
import me.soknight.easydesk.api.service.WorkspaceService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi

class WorkspaceRoutesTest {

    private val authenticator = mockk<ApiAuthenticator>()
    private val workspaceService = mockk<WorkspaceService>()

    @BeforeTest
    fun setUp() {
        clearMocks(authenticator, workspaceService)
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false })
        }
        routing {
            with(WorkspaceRoutes(authenticator, workspaceService)) { configureRoutes() }
        }
        block()
    }

    private fun stubServiceDefaults() {
        coEvery { workspaceService.getMetrics() } returns WorkspaceMetrics(
            avgResponseTime = 0.0,
            ticketsCounters = TicketsCounters(closed = 0, inProgress = 0, merged = 0, open = 0, resolved = 0),
        )
        coEvery { workspaceService.getSuperadminId() } returns null
        every { workspaceService.workspaceName } returns "Test Workspace"
        every { workspaceService.startedAt } returns 1716000000000L
        every { workspaceService.version } returns "test"
    }

    @Test
    fun `GET workspace returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/workspace")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET workspace returns 200 when authenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        stubServiceDefaults()

        val response = client.get("/api/v1/workspace")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET workspace calls getMetrics and getSuperadminId when authenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        stubServiceDefaults()

        client.get("/api/v1/workspace")

        coVerify { workspaceService.getMetrics() }
        coVerify { workspaceService.getSuperadminId() }
    }

}
