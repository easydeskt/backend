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
import me.soknight.easydesk.service.tickets.data.repository.TicketTagRepository
import kotlin.test.*

class TagRoutesTest {

    private val authenticator = mockk<ApiAuthenticator>()
    private val ticketRepository = mockk<TicketRepository>()
    private val ticketTagRepository = mockk<TicketTagRepository>()

    @BeforeTest
    fun setUp() {
        clearMocks(authenticator, ticketRepository, ticketTagRepository)
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false })
        }
        routing {
            with(TagRoutes(authenticator, ticketRepository, ticketTagRepository)) { configureRoutes() }
        }
        block()
    }

    // --- GET /api/v1/tags ---

    @Test
    fun `GET tags returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tags")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET tags returns 200 with empty list when no tags exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketTagRepository.findAll() } returns emptyList()

        val response = client.get("/api/v1/tags")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET tags returns 200 with tag list`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketTagRepository.findAll() } returns listOf(TestFixtures.tag(1L, "Bug"), TestFixtures.tag(2L, "Feature"))

        val response = client.get("/api/v1/tags")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tags ---

    @Test
    fun `POST tags returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bug"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST tags returns 403 when caller is operator role`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.post("/api/v1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bug"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST tags returns 400 when name is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.post("/api/v1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"  "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST tags returns 201 when admin creates tag without color`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { ticketTagRepository.create("Bug", null) } returns TestFixtures.tag(1L, "Bug")

        val response = client.post("/api/v1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bug"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST tags returns 201 when admin creates tag with color`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { ticketTagRepository.create("Bug", -65536) } returns TestFixtures.tag(1L, "Bug").copy(color = -65536)

        val response = client.post("/api/v1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bug","color":-65536}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    // --- PUT /api/v1/tags/{id} ---

    @Test
    fun `PUT tag returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.put("/api/v1/tags/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT tag returns 403 when caller is operator role`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.put("/api/v1/tags/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT tag returns 400 when id is not a number`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.put("/api/v1/tags/not-a-number") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT tag returns 400 when name is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.put("/api/v1/tags/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT tag returns 404 when tag does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { ticketTagRepository.update(1L, "Updated", null) } returns null

        val response = client.put("/api/v1/tags/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT tag returns 200 when admin updates tag`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { ticketTagRepository.update(1L, "Updated", null) } returns TestFixtures.tag(1L, "Updated")

        val response = client.put("/api/v1/tags/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- DELETE /api/v1/tags/{id} ---

    @Test
    fun `DELETE tag returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.delete("/api/v1/tags/1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE tag returns 403 when caller is operator role`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.delete("/api/v1/tags/1")

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE tag returns 400 when id is not a number`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.delete("/api/v1/tags/not-a-number")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `DELETE tag returns 404 when tag does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { ticketTagRepository.delete(1L) } returns false

        val response = client.delete("/api/v1/tags/1")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE tag returns 204 when admin deletes tag`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { ticketTagRepository.delete(1L) } returns true

        val response = client.delete("/api/v1/tags/1")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    // --- GET /api/v1/tickets/{id}/tags ---

    @Test
    fun `GET ticket tags returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tickets/1/tags")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET ticket tags returns 400 when ticket id is not a number`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.get("/api/v1/tickets/not-a-number/tags")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET ticket tags returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.get("/api/v1/tickets/1/tags")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET ticket tags returns 200 with tags list`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()
        coEvery { ticketTagRepository.findByTicket(1L) } returns listOf(TestFixtures.tag())

        val response = client.get("/api/v1/tickets/1/tags")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/tags ---

    @Test
    fun `POST ticket tag returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":1}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST ticket tag returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":1}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST ticket tag returns 404 when tag does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()
        coEvery { ticketTagRepository.findById(99L) } returns null

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":99}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST ticket tag returns 204 when tag added to ticket`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()
        coEvery { ticketTagRepository.findById(1L) } returns TestFixtures.tag()
        coEvery { ticketTagRepository.addToTicket(1L, 1L, TestFixtures.operatorId) } just Runs

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":1}""")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    // --- DELETE /api/v1/tickets/{id}/tags/{tagId} ---

    @Test
    fun `DELETE ticket tag returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.delete("/api/v1/tickets/1/tags/1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE ticket tag returns 400 when ticket id is not a number`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.delete("/api/v1/tickets/abc/tags/1")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `DELETE ticket tag returns 404 when tag was not on ticket`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketTagRepository.removeFromTicket(1L, 99L) } returns false

        val response = client.delete("/api/v1/tickets/1/tags/99")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE ticket tag returns 204 when tag removed from ticket`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketTagRepository.removeFromTicket(1L, 1L) } returns true

        val response = client.delete("/api/v1/tickets/1/tags/1")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

}
