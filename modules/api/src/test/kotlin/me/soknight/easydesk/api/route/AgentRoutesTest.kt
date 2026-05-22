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
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.api.model.Agent.Role
import kotlin.test.*

class AgentRoutesTest {

    private val agentRepository = mockk<AgentRepository>()
    private val authenticator = mockk<ApiAuthenticator>()

    @BeforeTest
    fun setUp() {
        clearMocks(agentRepository, authenticator)
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false })
        }
        routing {
            with(AgentRoutes(agentRepository, authenticator)) { configureRoutes() }
        }
        block()
    }

    // --- GET /api/v1/agents ---

    @Test
    fun `GET agents returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/agents")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET agents returns 200 with agent list when authenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { agentRepository.findAll(true) } returns listOf(TestFixtures.operatorAgent)
        coEvery { agentRepository.findAllBindingAttributes(any()) } returns emptyMap()

        val response = client.get("/api/v1/agents")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET agents with activeOnly false returns all agents`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { agentRepository.findAll(false) } returns listOf(TestFixtures.operatorAgent)
        coEvery { agentRepository.findAllBindingAttributes(any()) } returns emptyMap()

        val response = client.get("/api/v1/agents?activeOnly=false")

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { agentRepository.findAll(false) }
    }

    // --- GET /api/v1/agents/{id} ---

    @Test
    fun `GET agent by id returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/agents/${TestFixtures.operatorId}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET agent by id returns 400 when id is not a valid UUID`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.get("/api/v1/agents/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET agent by id returns 404 when agent does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { agentRepository.findById(TestFixtures.operatorId) } returns null

        val response = client.get("/api/v1/agents/${TestFixtures.operatorId}")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET agent by id returns 200 when agent exists`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { agentRepository.findById(TestFixtures.operatorId) } returns TestFixtures.operatorAgent
        coEvery { agentRepository.findBindingAttributes(TestFixtures.operatorId, any()) } returns null

        val response = client.get("/api/v1/agents/${TestFixtures.operatorId}")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/agents ---

    @Test
    fun `POST agents returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/agents") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"New Agent"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST agents returns 403 when caller is operator role`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.post("/api/v1/agents") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"New Agent"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST agents returns 400 when display_name is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.post("/api/v1/agents") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"   "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST agents returns 400 when role key is invalid`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.post("/api/v1/agents") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"New Agent","role":"superuser"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST agents returns 201 when admin creates operator agent`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { agentRepository.create("New Agent", Role.OPERATOR, TestFixtures.adminId) } returns
            TestFixtures.operatorAgent.copy(displayName = "New Agent")

        val response = client.post("/api/v1/agents") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"New Agent","role":"operator"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST agents returns 201 when admin creates admin agent`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { agentRepository.create("New Admin", Role.ADMIN, TestFixtures.adminId) } returns
            TestFixtures.adminAgent.copy(displayName = "New Admin")

        val response = client.post("/api/v1/agents") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"New Admin","role":"admin"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    // --- PATCH /api/v1/agents/{id} ---

    @Test
    fun `PATCH agent returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PATCH agent returns 403 when caller is operator role`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PATCH agent returns 400 when id is not a valid UUID`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.patch("/api/v1/agents/not-valid") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PATCH agent returns 400 when display_name is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PATCH agent returns 400 when role key is invalid`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"role":"superuser"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PATCH agent returns 404 when agent does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { agentRepository.update(TestFixtures.operatorId, "Updated", null, null) } returns null

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PATCH agent returns 200 when admin updates display name`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { agentRepository.update(TestFixtures.operatorId, "Updated", null, null) } returns
            TestFixtures.operatorAgent.copy(displayName = "Updated")

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"display_name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PATCH agent returns 200 when admin deactivates agent`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { agentRepository.update(TestFixtures.operatorId, null, false, null) } returns
            TestFixtures.operatorAgent.copy(isActive = false)

        val response = client.patch("/api/v1/agents/${TestFixtures.operatorId}") {
            contentType(ContentType.Application.Json)
            setBody("""{"is_active":false}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

}
