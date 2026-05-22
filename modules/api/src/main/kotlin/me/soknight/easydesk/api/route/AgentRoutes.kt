@file:OptIn(ExperimentalKtorApi::class, ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.toResponse
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.api.model.Agent.Role
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import org.koin.core.annotation.Single

@Serializable
data class CreateAgentRequest(
    @SerialName("display_name") val displayName: String,
    val role: String = "operator",
)

@Serializable
data class PatchAgentRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    val role: String? = null,
)

@Single
class AgentRoutes(
    private val agentRepository: AgentRepository,
    private val authenticator: ApiAuthenticator,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/agents") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val activeOnly = call.request.queryParameters["activeOnly"] != "false"
                val agents = agentRepository.findAll(activeOnly)
                val bindingAttrs = agentRepository.findAllBindingAttributes(TelegramSupervisorBrand)
                call.respond(HttpStatusCode.OK, agents.map { it.toResponse(bindingAttrs[it.identifier]?.usernameAttribute()) })
            }.describe {
                summary = "List agents"
                parameters {
                    query("activeOnly") { description = "Filter to active agents only; defaults to true" }
                }
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                }
            }

            get("{id}") {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.let { runCatching { Uuid.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val agent = agentRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val bindingAttrs = agentRepository.findBindingAttributes(id, TelegramSupervisorBrand)
                call.respond(HttpStatusCode.OK, agent.toResponse(bindingAttrs?.usernameAttribute()))
            }.describe {
                summary = "Get agent by ID"
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
                if (!principal.isAdmin) {
                    return@post call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val req = call.receive<CreateAgentRequest>()
                if (req.displayName.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val role = Role.entries.firstOrNull { it.key == req.role }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val agent = agentRepository.create(
                    displayName = req.displayName.trim(),
                    role = role,
                    addedByAgentId = principal.agent.identifier,
                )
                call.respond(HttpStatusCode.Created, agent.toResponse())
            }.describe {
                summary = "Create agent"
                responses {
                    HttpStatusCode.Created { description = "Created" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                }
            }

            patch("{id}") {
                val principal = authenticator.authenticate(call)
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@patch call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val id = call.parameters["id"]?.let { runCatching { Uuid.parse(it) }.getOrNull() }
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val req = call.receive<PatchAgentRequest>()
                if (req.displayName != null && req.displayName.isBlank()) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val role = req.role?.let { key ->
                    Role.entries.firstOrNull { it.key == key }
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val updated = agentRepository.update(
                    id = id,
                    displayName = req.displayName?.trim(),
                    isActive = req.isActive,
                    role = role,
                ) ?: return@patch call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, updated.toResponse())
            }.describe {
                summary = "Partially update agent"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Agents") }
    }

}

private fun JsonObject.usernameAttribute(): String? = get("username")?.jsonPrimitive?.content
