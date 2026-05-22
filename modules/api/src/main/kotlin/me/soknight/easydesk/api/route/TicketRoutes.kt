@file:OptIn(ExperimentalKtorApi::class, ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.NoteResponse
import me.soknight.easydesk.api.response.toDetailResponse
import me.soknight.easydesk.api.response.toNoteResponse
import me.soknight.easydesk.api.response.toResponse
import me.soknight.easydesk.api.response.toSummaryResponse
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.audit.data.repository.AuditEventRepository
import me.soknight.easydesk.service.channels.data.domain.Conversation
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.data.repository.IdentityNoteRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.templates.data.repository.ReplyTemplateRepository
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageStats
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketNoteRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketTagRepository
import me.soknight.easydesk.supervisor.api.event.TicketMessageEvent
import me.soknight.easydesk.supervisor.api.model.Ticket.Priority
import me.soknight.easydesk.supervisor.api.model.Ticket.Status
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.registry.TelegramTopicRegistry
import org.koin.core.annotation.Single

@Serializable data class PostAssignRequest(@SerialName("agent_id") val agentId: String?)
@Serializable data class PostMergeRequest(@SerialName("target_ticket_id") val targetTicketId: Long)
@Serializable data class PostNoteRequest(val scope: String, val text: String)
@Serializable data class PostPriorityRequest(val priority: String)
@Serializable data class PostTagsRequest(@SerialName("tag_ids") val tagIds: List<Long>)
@Serializable data class PutNoteRequest(val scope: String, val text: String)
@Serializable data class SendTemplateRequest(@SerialName("template_id") val templateId: Long)

@Single
class TicketRoutes(
    private val agentRepository: AgentRepository,
    private val auditEventRepository: AuditEventRepository,
    private val authenticator: ApiAuthenticator,
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val channelRepository: ChannelRepository,
    private val conversationRegistry: ConversationRegistry,
    private val conversationRepository: ConversationRepository,
    private val eventBus: EventBus,
    private val identityNoteRepository: IdentityNoteRepository,
    private val telegramSupervisorConfig: TelegramSupervisorConfig,
    private val telegramTopicRegistry: TelegramTopicRegistry,
    private val templateRepository: ReplyTemplateRepository,
    private val ticketMessageRepository: TicketMessageRepository,
    private val ticketNoteRepository: TicketNoteRepository,
    private val ticketRepository: TicketRepository,
    private val ticketTagRepository: TicketTagRepository,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/tickets") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)

                val assignedAgentIdFilter = call.request.queryParameters["assignedAgentId"]
                val priorityFilter = call.request.queryParameters["priority"]
                val statusFilter = call.request.queryParameters["status"]
                val tagIdFilter = call.request.queryParameters["tagId"]?.toLongOrNull()

                var tickets = ticketRepository.findAll()

                if (assignedAgentIdFilter != null) {
                    tickets = tickets.filter { it.assignedAgentId?.toString() == assignedAgentIdFilter }
                }
                if (priorityFilter != null) {
                    tickets = tickets.filter { it.priority.key == priorityFilter }
                }
                if (statusFilter != null) {
                    tickets = tickets.filter { it.status.key == statusFilter }
                }
                if (tagIdFilter != null) {
                    val tagTicketIds = ticketTagRepository.findTicketIdsByTag(tagIdFilter).toSet()
                    tickets = tickets.filter { it.identifier in tagTicketIds }
                }

                val responses = tickets.map { ticket ->
                    val conv = conversationRepository.findById(ticket.conversationId)!!
                    ticket.toSummaryResponse(
                        channel = channelRepository.findById(conv.channelId)!!,
                        identity = channelIdentityRepository.findById(conv.identityId)!!,
                        stats = ticketMessageRepository.getStats(ticket.identifier, ticket.readUpToMessageId),
                        topicUrl = buildTopicUrl(ticket.identifier),
                        tags = ticketTagRepository.findByTicket(ticket.identifier).map { it.toResponse() },
                    )
                }
                call.respond(HttpStatusCode.OK, responses)
            }.describe {
                summary = "List tickets"
                parameters {
                    query("assignedAgentId") { description = "Filter by assigned agent UUID" }
                    query("priority") { description = "Filter by priority key" }
                    query("status") { description = "Filter by status key" }
                    query("tagId") { description = "Filter by tag ID" }
                }
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                }
            }

            get("{id}") {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, buildDetailResponse(ticket))
            }.describe {
                summary = "Get ticket by ID"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            post("{id}/assign") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (ticket.status == Status.MERGED) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                val req = call.receive<PostAssignRequest>()
                val agentId = req.agentId?.let {
                    runCatching { Uuid.parse(it) }.getOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val previousAgentId = ticket.assignedAgentId
                val updated = ticketRepository.assign(id, agentId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (agentId != null) {
                    val eventType = if (previousAgentId != null) "ticket.takeover" else "ticket.assigned"
                    auditEventRepository.create(
                        eventType = eventType,
                        ticketId = id,
                        agentId = principal.agent.identifier,
                        payload = buildJsonObject {
                            put("agent_id", agentId.toString())
                            if (previousAgentId != null) put("previous_agent_id", previousAgentId.toString())
                        },
                    )
                }
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Assign or unassign agent"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.Conflict { description = "Conflict" }
                }
            }

            post("{id}/close") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (ticket.status != Status.OPEN && ticket.status != Status.IN_PROGRESS) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                val updated = ticketRepository.close(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                auditEventRepository.create("ticket.closed", id, principal.agent.identifier)
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Close ticket"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.Conflict { description = "Conflict" }
                }
            }

            post("{id}/free") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (ticket.status != Status.IN_PROGRESS) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                val previousAgentId = ticket.assignedAgentId
                ticketRepository.free(id)
                val updated = ticketRepository.updateStatus(id, Status.OPEN)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                auditEventRepository.create(
                    eventType = "ticket.freed",
                    ticketId = id,
                    agentId = principal.agent.identifier,
                    payload = buildJsonObject {
                        if (previousAgentId != null) put("previous_agent_id", previousAgentId.toString())
                    },
                )
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Free ticket assignment"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.Conflict { description = "Conflict" }
                }
            }

            post("{id}/merge") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val source = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (source.status != Status.OPEN && source.status != Status.IN_PROGRESS) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                val req = call.receive<PostMergeRequest>()
                if (req.targetTicketId == id) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val target = ticketRepository.findById(req.targetTicketId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (source.conversationId != target.conversationId) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                val (_, mergedTarget) = ticketRepository.merge(id, req.targetTicketId, principal.agent.identifier)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                auditEventRepository.create(
                    eventType = "ticket.merged",
                    ticketId = id,
                    agentId = principal.agent.identifier,
                    payload = buildJsonObject { put("target_ticket_id", req.targetTicketId) },
                )
                call.respond(HttpStatusCode.OK, buildDetailResponse(mergedTarget))
            }.describe {
                summary = "Merge ticket into another"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.Conflict { description = "Conflict" }
                }
            }

            post("{id}/priority") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val req = call.receive<PostPriorityRequest>()
                val priority = Priority.entries.firstOrNull { it.key == req.priority }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val previousPriority = ticket.priority
                val updated = ticketRepository.updatePriority(id, priority)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                auditEventRepository.create(
                    eventType = "ticket.priority_changed",
                    ticketId = id,
                    agentId = principal.agent.identifier,
                    payload = buildJsonObject {
                        put("priority", priority.key)
                        put("previous_priority", previousPriority.key)
                    },
                )
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Change ticket priority"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            post("{id}/reopen") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (ticket.status != Status.RESOLVED && ticket.status != Status.CLOSED) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                ticketRepository.reopen(id, principal.agent.identifier)
                val updated = ticketRepository.assign(id, principal.agent.identifier)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                auditEventRepository.create("ticket.reopened", id, principal.agent.identifier)
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Reopen ticket"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.Conflict { description = "Conflict" }
                }
            }

            post("{id}/reply-template") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val req = call.receive<SendTemplateRequest>()
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val template = templateRepository.findById(req.templateId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val content = template.content
                    ?: return@post call.respond(HttpStatusCode.UnprocessableEntity, ServerErrorDto.UnprocessableEntity)
                val conversation = conversationRegistry.getOrNull(ticket.conversationId)
                    ?: return@post call.respond(HttpStatusCode.ServiceUnavailable, ServerErrorDto.ServiceUnavailable)
                val sent = conversation.send { plainText = content }
                val ticketMessage = ticketMessageRepository.create(
                    ticketId = id,
                    nativeId = sent.nativeId,
                    senderKind = ActorKind.AGENT,
                    senderAgentId = principal.agent.identifier,
                    senderIdentityId = null,
                    plainText = content,
                    inReplyToNativeId = null,
                    platformTimestamp = Clock.System.now(),
                )
                eventBus.publish(TicketMessageEvent.Recorded(
                    conversationId = ticket.conversationId,
                    message = ticketMessage,
                ))
                call.respond(HttpStatusCode.OK, ticketMessage.toResponse())
            }.describe {
                summary = "Send reply template to client"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.UnprocessableEntity { description = "Unprocessable entity" }
                    HttpStatusCode.ServiceUnavailable { description = "Service unavailable" }
                }
            }

            post("{id}/resolve") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                if (ticket.status != Status.IN_PROGRESS) {
                    return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                }
                val updated = ticketRepository.resolve(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                auditEventRepository.create("ticket.resolved", id, principal.agent.identifier)
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Resolve ticket"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.Conflict { description = "Conflict" }
                }
            }

            post("{id}/tags") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                if (ticketRepository.findById(id) == null) {
                    return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                val req = call.receive<PostTagsRequest>()
                val previousTagIds = ticketTagRepository.findByTicket(id).map { it.identifier }.toSet()
                ticketTagRepository.setTags(id, req.tagIds, principal.agent.identifier)
                val newTagIds = req.tagIds.toSet()
                for (tagId in newTagIds - previousTagIds) {
                    auditEventRepository.create(
                        eventType = "ticket.tag_added",
                        ticketId = id,
                        agentId = principal.agent.identifier,
                        payload = buildJsonObject { put("tag_id", tagId) },
                    )
                }
                for (tagId in previousTagIds - newTagIds) {
                    auditEventRepository.create(
                        eventType = "ticket.tag_removed",
                        ticketId = id,
                        agentId = principal.agent.identifier,
                        payload = buildJsonObject { put("tag_id", tagId) },
                    )
                }
                val ticket = ticketRepository.findById(id)!!
                call.respond(HttpStatusCode.OK, buildDetailResponse(ticket))
            }.describe {
                summary = "Bulk-set ticket tags"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            patch("{id}/attributes") {
                authenticator.authenticate(call)
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val patch = call.receive<JsonObject>()
                val updated = ticketRepository.updateAttributes(id, patch, replace = false)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Merge ticket attributes"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            put("{id}/attributes") {
                authenticator.authenticate(call)
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val replacement = call.receive<JsonObject>()
                val updated = ticketRepository.updateAttributes(id, replacement, replace = true)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, buildDetailResponse(updated))
            }.describe {
                summary = "Replace ticket attributes"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Tickets") }

        route("/api/v1/tickets/{id}/history") {
            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                if (ticketRepository.findById(id) == null) {
                    return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                val events = auditEventRepository.findByTicket(id).map { event ->
                    val agentName = event.agentId?.let { agentRepository.findById(it)?.displayName }
                    event.toResponse(agentName)
                }
                call.respond(HttpStatusCode.OK, events)
            }.describe {
                summary = "Get ticket audit history"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }
        }.describe { tag("Tickets") }

        route("/api/v1/tickets/{id}/messages") {
            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                if (ticketRepository.findById(id) == null) {
                    return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                val messages = ticketMessageRepository.findByTicket(id).map { it.toResponse() }
                call.respond(HttpStatusCode.OK, messages)
            }.describe {
                summary = "Get ticket message thread"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }
        }.describe { tag("Tickets") }

        route("/api/v1/tickets/{id}/notes") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val scope = call.request.queryParameters["scope"] ?: "all"
                if (scope !in listOf("all", "client", "ticket")) {
                    return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val ticket = ticketRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val conv = conversationRepository.findById(ticket.conversationId)!!
                call.respond(HttpStatusCode.OK, buildNotes(id, conv, scope))
            }.describe {
                summary = "List ticket notes"
                parameters {
                    query("scope") { description = "Note scope: all, client, or ticket; defaults to all" }
                }
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            post {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val ticket = ticketRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val req = call.receive<PostNoteRequest>()
                if (req.text.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val note: NoteResponse = when (req.scope) {
                    "ticket" -> {
                        val created = ticketNoteRepository.create(id, req.text.trim(), principal.agent.identifier)
                        created.toNoteResponse(principal.agent.displayName)
                    }
                    "client" -> {
                        val conv = conversationRepository.findById(ticket.conversationId)!!
                        val created = identityNoteRepository.create(conv.identityId, req.text.trim(), principal.agent.identifier)
                        created.toNoteResponse(principal.agent.displayName)
                    }
                    else -> return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                call.respond(HttpStatusCode.Created, note)
            }.describe {
                summary = "Create ticket note"
                responses {
                    HttpStatusCode.Created { description = "Created" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            put("{noteId}") {
                authenticator.authenticate(call)
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val noteId = call.parameters["noteId"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                if (ticketRepository.findById(id) == null) {
                    return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                val req = call.receive<PutNoteRequest>()
                if (req.text.isBlank()) {
                    return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val note: NoteResponse = when (req.scope) {
                    "ticket" -> {
                        val existing = ticketNoteRepository.findById(noteId)
                        if (existing == null || existing.ticketId != id) {
                            return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                        }
                        val updated = ticketNoteRepository.update(noteId, req.text.trim())
                            ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                        val agentName = agentRepository.findById(updated.authorAgentId)?.displayName ?: ""
                        updated.toNoteResponse(agentName)
                    }
                    "client" -> {
                        val ticket = ticketRepository.findById(id)!!
                        val conv = conversationRepository.findById(ticket.conversationId)!!
                        val existing = identityNoteRepository.findById(noteId)
                        if (existing == null || existing.identityId != conv.identityId) {
                            return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                        }
                        val updated = identityNoteRepository.update(noteId, req.text.trim())
                            ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                        val agentName = agentRepository.findById(updated.authorAgentId)?.displayName ?: ""
                        updated.toNoteResponse(agentName)
                    }
                    else -> return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                call.respond(HttpStatusCode.OK, note)
            }.describe {
                summary = "Update ticket note"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            delete("{noteId}") {
                authenticator.authenticate(call)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val noteId = call.parameters["noteId"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val scope = call.request.queryParameters["scope"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                if (ticketRepository.findById(id) == null) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                val deleted = when (scope) {
                    "ticket" -> {
                        val existing = ticketNoteRepository.findById(noteId)
                        if (existing == null || existing.ticketId != id) {
                            return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                        }
                        ticketNoteRepository.delete(noteId)
                    }
                    "client" -> {
                        val ticket = ticketRepository.findById(id)!!
                        val conv = conversationRepository.findById(ticket.conversationId)!!
                        val existing = identityNoteRepository.findById(noteId)
                        if (existing == null || existing.identityId != conv.identityId) {
                            return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                        }
                        identityNoteRepository.delete(noteId)
                    }
                    else -> return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                if (!deleted) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Delete ticket note"
                parameters {
                    query("scope") { description = "Note scope: client or ticket (required)" }
                }
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Ticket notes") }

    }

    private fun buildTopicUrl(ticketId: Long): String? {
        val topicId = telegramTopicRegistry.getOrNull(ticketId) ?: return null
        val internalId = -telegramSupervisorConfig.supergroupId - 1_000_000_000_000L
        return "https://t.me/c/$internalId/$topicId"
    }

    private suspend fun buildDetailResponse(ticket: Ticket) = run {
        val conv = conversationRepository.findById(ticket.conversationId)!!
        ticket.toDetailResponse(
            channel = channelRepository.findById(conv.channelId)!!,
            identity = channelIdentityRepository.findById(conv.identityId)!!,
            stats = ticketMessageRepository.getStats(ticket.identifier, ticket.readUpToMessageId),
            topicUrl = buildTopicUrl(ticket.identifier),
            tags = ticketTagRepository.findByTicket(ticket.identifier).map { it.toResponse() },
            notes = buildNotes(ticket.identifier, conv),
        )
    }

    private suspend fun buildNotes(ticketId: Long, conv: Conversation, scope: String = "all"): List<NoteResponse> {
        val ticketNotes = if (scope == "all" || scope == "ticket") {
            ticketNoteRepository.findByTicket(ticketId).map { note ->
                val agentName = agentRepository.findById(note.authorAgentId)?.displayName ?: ""
                note.toNoteResponse(agentName)
            }
        } else emptyList()
        val identityNotes = if (scope == "all" || scope == "client") {
            identityNoteRepository.findByIdentity(conv.identityId).map { note ->
                val agentName = agentRepository.findById(note.authorAgentId)?.displayName ?: ""
                note.toNoteResponse(agentName)
            }
        } else emptyList()
        return (ticketNotes + identityNotes).sortedBy { it.createdAt }
    }

}
