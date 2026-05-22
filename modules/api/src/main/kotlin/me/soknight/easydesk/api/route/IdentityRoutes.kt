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
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.IdentityNoteRepository
import org.koin.core.annotation.Single

@Serializable
data class CreateNoteRequest(val text: String)

@Serializable
data class UpdateNoteRequest(val text: String)

@Single
class IdentityRoutes(
    private val authenticator: ApiAuthenticator,
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val identityNoteRepository: IdentityNoteRepository,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/identities/{id}") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val identity = channelIdentityRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, identity.toResponse())
            }.describe {
                summary = "Get channel identity"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            route("notes") {

                get {
                    authenticator.authenticate(call)
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                    if (channelIdentityRepository.findById(id) == null) {
                        return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                    }
                    call.respond(HttpStatusCode.OK, identityNoteRepository.findByIdentity(id).map { it.toResponse() })
                }.describe {
                    summary = "List identity notes"
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
                    if (channelIdentityRepository.findById(id) == null) {
                        return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                    }
                    val req = call.receive<CreateNoteRequest>()
                    if (req.text.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                    }
                    val note = identityNoteRepository.create(id, req.text.trim(), principal.agent.identifier)
                    call.respond(HttpStatusCode.Created, note.toResponse())
                }.describe {
                    summary = "Create identity note"
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
                    val existingNote = identityNoteRepository.findById(noteId)
                    if (existingNote == null || existingNote.identityId != id) {
                        return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                    }
                    val req = call.receive<UpdateNoteRequest>()
                    if (req.text.isBlank()) {
                        return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                    }
                    val updated = identityNoteRepository.update(noteId, req.text.trim())
                        ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                    call.respond(HttpStatusCode.OK, updated.toResponse())
                }.describe {
                    summary = "Update identity note"
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
                    val existingNote = identityNoteRepository.findById(noteId)
                    if (existingNote == null || existingNote.identityId != id) {
                        return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                    }
                    val deleted = identityNoteRepository.delete(noteId)
                    if (!deleted) {
                        return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                    }
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Delete identity note"
                    responses {
                        HttpStatusCode.NoContent { description = "No content" }
                        HttpStatusCode.BadRequest { description = "Bad request" }
                        HttpStatusCode.Unauthorized { description = "Unauthorized" }
                        HttpStatusCode.NotFound { description = "Not found" }
                    }
                }

            }

        }.describe { tag("Identities") }
    }

}
