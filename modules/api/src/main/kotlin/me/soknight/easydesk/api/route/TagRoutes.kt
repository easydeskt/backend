@file:OptIn(ExperimentalKtorApi::class, ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.Serializable
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.toResponse
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketTagRepository
import org.koin.core.annotation.Single

@Serializable data class CreateTagRequest(val color: Int? = null, val name: String)
@Serializable data class UpdateTagRequest(val color: Int? = null, val name: String)
@Serializable data class AddTagToTicketRequest(val id: Long)

@Single
class TagRoutes(
    private val authenticator: ApiAuthenticator,
    private val ticketRepository: TicketRepository,
    private val ticketTagRepository: TicketTagRepository,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/tags") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                call.respond(HttpStatusCode.OK, ticketTagRepository.findAll().map { it.toResponse() })
            }.describe {
                summary = "List ticket tags"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                }
            }

            post {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@post call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val req = call.receive<CreateTagRequest>()
                if (req.name.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val tag = ticketTagRepository.create(req.name.trim(), req.color)
                call.respond(HttpStatusCode.Created, tag.toResponse())
            }.describe {
                summary = "Create tag"
                responses {
                    HttpStatusCode.Created { description = "Created" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                }
            }

            put("{id}") {
                val principal = authenticator.authenticate(call)
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@put call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val req = call.receive<UpdateTagRequest>()
                if (req.name.isBlank()) {
                    return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val updated = ticketTagRepository.update(id, req.name.trim(), req.color)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, updated.toResponse())
            }.describe {
                summary = "Update tag"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            delete("{id}") {
                val principal = authenticator.authenticate(call)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val deleted = ticketTagRepository.delete(id)
                if (!deleted) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Delete tag"
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Tags") }

        route("/api/v1/tickets/{id}/tags") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                if (ticketRepository.findById(id) == null) {
                    return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                call.respond(HttpStatusCode.OK, ticketTagRepository.findByTicket(id).map { it.toResponse() })
            }.describe {
                summary = "List tags on ticket"
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
                if (ticketRepository.findById(id) == null) {
                    return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                val req = call.receive<AddTagToTicketRequest>()
                if (ticketTagRepository.findById(req.id) == null) {
                    return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                ticketTagRepository.addToTicket(id, req.id, principal.agent.identifier)
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Add tag to ticket"
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            delete("{tagId}") {
                authenticator.authenticate(call)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val tagId = call.parameters["tagId"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val removed = ticketTagRepository.removeFromTicket(id, tagId)
                if (!removed) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Remove tag from ticket"
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Tags") }
    }

}
