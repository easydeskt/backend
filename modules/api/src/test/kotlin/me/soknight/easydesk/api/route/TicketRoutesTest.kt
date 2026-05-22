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
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.audit.data.repository.AuditEventRepository
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.data.repository.IdentityNoteRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.templates.data.repository.ReplyTemplateRepository
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketNoteRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketTagRepository
import me.soknight.easydesk.supervisor.api.model.Ticket as SupervisorTicket
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.registry.TelegramTopicRegistry
import kotlin.test.*

class TicketRoutesTest {

    private val agentRepository = mockk<AgentRepository>()
    private val auditEventRepository = mockk<AuditEventRepository>()
    private val authenticator = mockk<ApiAuthenticator>()
    private val channelIdentityRepository = mockk<ChannelIdentityRepository>()
    private val channelRepository = mockk<ChannelRepository>()
    private val conversationRegistry = mockk<ConversationRegistry>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val eventBus = mockk<EventBus>()
    private val identityNoteRepository = mockk<IdentityNoteRepository>()
    private val telegramConfig = mockk<TelegramSupervisorConfig>()
    private val telegramTopicRegistry = mockk<TelegramTopicRegistry>()
    private val templateRepository = mockk<ReplyTemplateRepository>()
    private val ticketMessageRepository = mockk<TicketMessageRepository>()
    private val ticketNoteRepository = mockk<TicketNoteRepository>()
    private val ticketRepository = mockk<TicketRepository>()
    private val ticketTagRepository = mockk<TicketTagRepository>()

    @BeforeTest
    fun setUp() {
        clearMocks(
            agentRepository, auditEventRepository, authenticator,
            channelIdentityRepository, channelRepository, conversationRegistry,
            conversationRepository, eventBus, identityNoteRepository,
            telegramConfig, telegramTopicRegistry, templateRepository,
            ticketMessageRepository, ticketNoteRepository, ticketRepository,
            ticketTagRepository,
        )
        every { telegramConfig.supergroupId } returns 0L
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false })
        }
        routing {
            with(TicketRoutes(
                agentRepository = agentRepository,
                auditEventRepository = auditEventRepository,
                authenticator = authenticator,
                channelIdentityRepository = channelIdentityRepository,
                channelRepository = channelRepository,
                conversationRegistry = conversationRegistry,
                conversationRepository = conversationRepository,
                eventBus = eventBus,
                identityNoteRepository = identityNoteRepository,
                telegramSupervisorConfig = telegramConfig,
                telegramTopicRegistry = telegramTopicRegistry,
                templateRepository = templateRepository,
                ticketMessageRepository = ticketMessageRepository,
                ticketNoteRepository = ticketNoteRepository,
                ticketRepository = ticketRepository,
                ticketTagRepository = ticketTagRepository,
            )) { configureRoutes() }
        }
        block()
    }

    // Sets up all mocks required by buildDetailResponse() for the given ticket
    private fun mockDetailResponse(ticket: Ticket) {
        coEvery { conversationRepository.findById(ticket.conversationId) } returns TestFixtures.conversation
        coEvery { channelRepository.findById(TestFixtures.conversation.channelId) } returns TestFixtures.channel
        coEvery { channelIdentityRepository.findById(TestFixtures.conversation.identityId) } returns TestFixtures.identity
        coEvery { ticketMessageRepository.getStats(ticket.identifier, any()) } returns TestFixtures.stats
        every { telegramTopicRegistry.getOrNull(ticket.identifier) } returns null
        coEvery { ticketTagRepository.findByTicket(ticket.identifier) } returns emptyList()
        coEvery { ticketNoteRepository.findByTicket(ticket.identifier) } returns emptyList()
        coEvery { identityNoteRepository.findByIdentity(TestFixtures.conversation.identityId) } returns emptyList()
    }

    // --- GET /api/v1/tickets ---

    @Test
    fun `GET tickets returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tickets")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET tickets returns 200 with empty list when no tickets`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findAll() } returns emptyList()

        val response = client.get("/api/v1/tickets")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET tickets returns 200 with ticket list`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findAll() } returns listOf(ticket)
        coEvery { conversationRepository.findById(ticket.conversationId) } returns TestFixtures.conversation
        coEvery { channelRepository.findById(TestFixtures.conversation.channelId) } returns TestFixtures.channel
        coEvery { channelIdentityRepository.findById(TestFixtures.conversation.identityId) } returns TestFixtures.identity
        coEvery { ticketMessageRepository.getStats(ticket.identifier, any()) } returns TestFixtures.stats
        every { telegramTopicRegistry.getOrNull(ticket.identifier) } returns null
        coEvery { ticketTagRepository.findByTicket(ticket.identifier) } returns emptyList()

        val response = client.get("/api/v1/tickets")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET tickets filters by status`() = withApp {
        val openTicket = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        val inProgressTicket = TestFixtures.ticket(id = 2L, status = SupervisorTicket.Status.IN_PROGRESS)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findAll() } returns listOf(openTicket, inProgressTicket)
        // Only the open ticket should be returned, so only its detail mocks are needed
        coEvery { conversationRepository.findById(openTicket.conversationId) } returns TestFixtures.conversation
        coEvery { channelRepository.findById(TestFixtures.conversation.channelId) } returns TestFixtures.channel
        coEvery { channelIdentityRepository.findById(TestFixtures.conversation.identityId) } returns TestFixtures.identity
        coEvery { ticketMessageRepository.getStats(openTicket.identifier, any()) } returns TestFixtures.stats
        every { telegramTopicRegistry.getOrNull(openTicket.identifier) } returns null
        coEvery { ticketTagRepository.findByTicket(openTicket.identifier) } returns emptyList()

        val response = client.get("/api/v1/tickets?status=open")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- GET /api/v1/tickets/{id} ---

    @Test
    fun `GET ticket by id returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tickets/1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET ticket by id returns 400 when id is not a number`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.get("/api/v1/tickets/not-a-number")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET ticket by id returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.get("/api/v1/tickets/1")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET ticket by id returns 200 with ticket detail`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        mockDetailResponse(ticket)

        val response = client.get("/api/v1/tickets/1")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/assign ---

    @Test
    fun `POST assign returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/assign") {
            contentType(ContentType.Application.Json)
            setBody("""{"agent_id":null}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST assign returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.post("/api/v1/tickets/1/assign") {
            contentType(ContentType.Application.Json)
            setBody("""{"agent_id":null}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST assign returns 409 when ticket is merged`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.MERGED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/assign") {
            contentType(ContentType.Application.Json)
            setBody("""{"agent_id":null}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST assign returns 400 when agent_id is not a valid UUID`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/assign") {
            contentType(ContentType.Application.Json)
            setBody("""{"agent_id":"not-a-uuid"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST assign returns 200 when ticket is assigned to agent`() = withApp {
        val ticket = TestFixtures.ticket()
        val assigned = TestFixtures.ticket(assignedAgentId = TestFixtures.operatorId)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.assign(1L, TestFixtures.operatorId) } returns assigned
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(assigned)

        val response = client.post("/api/v1/tickets/1/assign") {
            contentType(ContentType.Application.Json)
            setBody("""{"agent_id":"${TestFixtures.operatorId}"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST assign returns 200 when ticket is unassigned with null agent_id`() = withApp {
        val ticket = TestFixtures.ticket(assignedAgentId = TestFixtures.operatorId)
        val unassigned = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.assign(1L, null) } returns unassigned
        mockDetailResponse(unassigned)

        val response = client.post("/api/v1/tickets/1/assign") {
            contentType(ContentType.Application.Json)
            setBody("""{"agent_id":null}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/close ---

    @Test
    fun `POST close returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/close")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST close returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.post("/api/v1/tickets/1/close")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST close returns 409 when ticket is already resolved`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.RESOLVED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/close")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST close returns 409 when ticket is already closed`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.CLOSED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/close")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST close returns 200 when open ticket is closed`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        val closed = TestFixtures.ticket(status = SupervisorTicket.Status.CLOSED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.close(1L) } returns closed
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(closed)

        val response = client.post("/api/v1/tickets/1/close")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST close returns 200 when in-progress ticket is closed`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.IN_PROGRESS)
        val closed = TestFixtures.ticket(status = SupervisorTicket.Status.CLOSED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.close(1L) } returns closed
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(closed)

        val response = client.post("/api/v1/tickets/1/close")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/free ---

    @Test
    fun `POST free returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/free")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST free returns 409 when ticket is not in-progress`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/free")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST free returns 200 when in-progress ticket is freed`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.IN_PROGRESS, assignedAgentId = TestFixtures.operatorId)
        val freed = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.free(1L) } returns ticket
        coEvery { ticketRepository.updateStatus(1L, SupervisorTicket.Status.OPEN) } returns freed
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(freed)

        val response = client.post("/api/v1/tickets/1/free")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/resolve ---

    @Test
    fun `POST resolve returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/resolve")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST resolve returns 409 when ticket is not in-progress`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/resolve")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST resolve returns 200 when in-progress ticket is resolved`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.IN_PROGRESS)
        val resolved = TestFixtures.ticket(status = SupervisorTicket.Status.RESOLVED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.resolve(1L) } returns resolved
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(resolved)

        val response = client.post("/api/v1/tickets/1/resolve")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/reopen ---

    @Test
    fun `POST reopen returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/reopen")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST reopen returns 409 when ticket is open`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/reopen")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST reopen returns 409 when ticket is in-progress`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.IN_PROGRESS)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/reopen")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST reopen returns 200 when resolved ticket is reopened`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.RESOLVED)
        val reopened = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN, assignedAgentId = TestFixtures.operatorId)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.reopen(1L, TestFixtures.operatorId) } returns ticket
        coEvery { ticketRepository.assign(1L, TestFixtures.operatorId) } returns reopened
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(reopened)

        val response = client.post("/api/v1/tickets/1/reopen")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST reopen returns 200 when closed ticket is reopened`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.CLOSED)
        val reopened = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN, assignedAgentId = TestFixtures.operatorId)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.reopen(1L, TestFixtures.operatorId) } returns ticket
        coEvery { ticketRepository.assign(1L, TestFixtures.operatorId) } returns reopened
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(reopened)

        val response = client.post("/api/v1/tickets/1/reopen")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/merge ---

    @Test
    fun `POST merge returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/merge") {
            contentType(ContentType.Application.Json)
            setBody("""{"target_ticket_id":2}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST merge returns 409 when source ticket is not open or in-progress`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.RESOLVED)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/merge") {
            contentType(ContentType.Application.Json)
            setBody("""{"target_ticket_id":2}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST merge returns 400 when source and target are the same ticket`() = withApp {
        val ticket = TestFixtures.ticket(status = SupervisorTicket.Status.OPEN)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/merge") {
            contentType(ContentType.Application.Json)
            setBody("""{"target_ticket_id":1}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST merge returns 409 when tickets are in different conversations`() = withApp {
        val source = TestFixtures.ticket(id = 1L, status = SupervisorTicket.Status.OPEN, conversationId = 1L)
        val target = TestFixtures.ticket(id = 2L, conversationId = 2L)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns source
        coEvery { ticketRepository.findById(2L) } returns target

        val response = client.post("/api/v1/tickets/1/merge") {
            contentType(ContentType.Application.Json)
            setBody("""{"target_ticket_id":2}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST merge returns 200 when tickets are merged`() = withApp {
        val source = TestFixtures.ticket(id = 1L, status = SupervisorTicket.Status.OPEN, conversationId = 1L)
        val target = TestFixtures.ticket(id = 2L, conversationId = 1L)
        val mergedTarget = TestFixtures.ticket(id = 2L, conversationId = 1L, status = SupervisorTicket.Status.OPEN)
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns source
        coEvery { ticketRepository.findById(2L) } returns target
        coEvery { ticketRepository.merge(1L, 2L, TestFixtures.operatorId) } returns Pair(source, mergedTarget)
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(mergedTarget)

        val response = client.post("/api/v1/tickets/1/merge") {
            contentType(ContentType.Application.Json)
            setBody("""{"target_ticket_id":2}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/priority ---

    @Test
    fun `POST priority returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/priority") {
            contentType(ContentType.Application.Json)
            setBody("""{"priority":"high"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST priority returns 400 when priority key is invalid`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket

        val response = client.post("/api/v1/tickets/1/priority") {
            contentType(ContentType.Application.Json)
            setBody("""{"priority":"critical"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST priority returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.post("/api/v1/tickets/1/priority") {
            contentType(ContentType.Application.Json)
            setBody("""{"priority":"high"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST priority returns 200 when priority is changed to high`() = withApp {
        val ticket = TestFixtures.ticket()
        val updated = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.updatePriority(1L, SupervisorTicket.Priority.HIGH) } returns updated
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(updated)

        val response = client.post("/api/v1/tickets/1/priority") {
            contentType(ContentType.Application.Json)
            setBody("""{"priority":"high"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST priority returns 200 when priority is changed to low`() = withApp {
        val ticket = TestFixtures.ticket()
        val updated = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketRepository.updatePriority(1L, SupervisorTicket.Priority.LOW) } returns updated
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(updated)

        val response = client.post("/api/v1/tickets/1/priority") {
            contentType(ContentType.Application.Json)
            setBody("""{"priority":"low"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/tags (bulk set) ---

    @Test
    fun `POST bulk tags returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"tag_ids":[1,2]}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST bulk tags returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"tag_ids":[1,2]}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST bulk tags returns 200 when tags are set`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketTagRepository.findByTicket(1L) } returns emptyList()
        coEvery { ticketTagRepository.setTags(1L, listOf(1L, 2L), TestFixtures.operatorId) } just Runs
        coEvery { auditEventRepository.create(any(), any(), any(), any()) } returns mockk()
        mockDetailResponse(ticket)

        val response = client.post("/api/v1/tickets/1/tags") {
            contentType(ContentType.Application.Json)
            setBody("""{"tag_ids":[1,2]}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- GET /api/v1/tickets/{id}/history ---

    @Test
    fun `GET ticket history returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tickets/1/history")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET ticket history returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.get("/api/v1/tickets/1/history")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET ticket history returns 200 with empty list`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()
        coEvery { auditEventRepository.findByTicket(1L) } returns emptyList()

        val response = client.get("/api/v1/tickets/1/history")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- GET /api/v1/tickets/{id}/messages ---

    @Test
    fun `GET ticket messages returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tickets/1/messages")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET ticket messages returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.get("/api/v1/tickets/1/messages")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET ticket messages returns 200 with empty list`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()
        coEvery { ticketMessageRepository.findByTicket(1L) } returns emptyList()

        val response = client.get("/api/v1/tickets/1/messages")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- GET /api/v1/tickets/{id}/notes ---

    @Test
    fun `GET ticket notes returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.get("/api/v1/tickets/1/notes")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET ticket notes returns 400 when scope is invalid`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal

        val response = client.get("/api/v1/tickets/1/notes?scope=invalid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET ticket notes returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.get("/api/v1/tickets/1/notes")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET ticket notes returns 200 with notes for all scopes`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { conversationRepository.findById(ticket.conversationId) } returns TestFixtures.conversation
        coEvery { ticketNoteRepository.findByTicket(1L) } returns emptyList()
        coEvery { identityNoteRepository.findByIdentity(TestFixtures.conversation.identityId) } returns emptyList()

        val response = client.get("/api/v1/tickets/1/notes")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET ticket notes returns 200 with only ticket notes when scope is ticket`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { conversationRepository.findById(ticket.conversationId) } returns TestFixtures.conversation
        coEvery { ticketNoteRepository.findByTicket(1L) } returns emptyList()

        val response = client.get("/api/v1/tickets/1/notes?scope=ticket")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- POST /api/v1/tickets/{id}/notes ---

    @Test
    fun `POST ticket note returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.post("/api/v1/tickets/1/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"scope":"ticket","text":"Note text"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST ticket note returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns null

        val response = client.post("/api/v1/tickets/1/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"scope":"ticket","text":"Note text"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST ticket note returns 400 when text is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()

        val response = client.post("/api/v1/tickets/1/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"scope":"ticket","text":"   "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST ticket note returns 400 when scope is invalid`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()

        val response = client.post("/api/v1/tickets/1/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"scope":"system","text":"Note text"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST ticket note returns 201 when ticket-scoped note is created`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns ticket
        coEvery { ticketNoteRepository.create(1L, "Note text", TestFixtures.operatorId) } returns mockk {
            every { identifier } returns 1L
            every { ticketId } returns 1L
            every { text } returns "Note text"
            every { authorAgentId } returns TestFixtures.operatorId
            every { createdAt } returns TestFixtures.now
            every { updatedAt } returns TestFixtures.now
        }

        val response = client.post("/api/v1/tickets/1/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"scope":"ticket","text":"Note text"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    // --- DELETE /api/v1/tickets/{id}/notes/{noteId} ---

    @Test
    fun `DELETE ticket note returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.delete("/api/v1/tickets/1/notes/1?scope=ticket")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE ticket note returns 400 when scope is missing`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()

        val response = client.delete("/api/v1/tickets/1/notes/1")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `DELETE ticket note returns 204 when ticket note is deleted`() = withApp {
        val note = mockk<me.soknight.easydesk.service.tickets.data.domain.TicketNote> {
            every { identifier } returns 1L
            every { ticketId } returns 1L
        }
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.findById(1L) } returns TestFixtures.ticket()
        coEvery { ticketNoteRepository.findById(1L) } returns note
        coEvery { ticketNoteRepository.delete(1L) } returns true

        val response = client.delete("/api/v1/tickets/1/notes/1?scope=ticket")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    // --- PATCH /api/v1/tickets/{id}/attributes ---

    @Test
    fun `PATCH attributes returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.patch("/api/v1/tickets/1/attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"value"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PATCH attributes returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.updateAttributes(1L, any(), false) } returns null

        val response = client.patch("/api/v1/tickets/1/attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"value"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PATCH attributes returns 200 when attributes are merged`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.updateAttributes(1L, any(), false) } returns ticket
        mockDetailResponse(ticket)

        val response = client.patch("/api/v1/tickets/1/attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"resolution_note":"Solved"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- PUT /api/v1/tickets/{id}/attributes ---

    @Test
    fun `PUT attributes returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null

        val response = client.put("/api/v1/tickets/1/attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"value"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT attributes returns 404 when ticket does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.updateAttributes(1L, any(), true) } returns null

        val response = client.put("/api/v1/tickets/1/attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"value"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT attributes returns 200 when attributes are replaced`() = withApp {
        val ticket = TestFixtures.ticket()
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        coEvery { ticketRepository.updateAttributes(1L, any(), true) } returns ticket
        mockDetailResponse(ticket)

        val response = client.put("/api/v1/tickets/1/attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

}
