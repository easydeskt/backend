@file:OptIn(ExperimentalKtorApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.toResponse
import me.soknight.easydesk.channel.api.ChannelProviderRegistry
import me.soknight.easydesk.channel.api.config.ConfigField
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import org.koin.core.annotation.Single

@Serializable
data class CreateChannelRequest(
    val brand: String,
    val config: JsonObject? = null,
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class UpdateChannelRequest(
    val config: JsonObject? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean? = null,
)

@Single
class ChannelRoutes(
    private val authenticator: ApiAuthenticator,
    private val channelRepository: ChannelRepository,
    private val providerRegistry: ChannelProviderRegistry,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/channels") {

            get("providers") {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                call.respond(HttpStatusCode.OK, providerRegistry.providers.map { it.toResponse() })
            }.describe {
                summary = "List channel provider types"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                }
            }

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val enabledOnly = call.request.queryParameters["enabledOnly"] != "false"
                call.respond(HttpStatusCode.OK, channelRepository.findAll(enabledOnly).map { it.toResponse(providerRegistry) })
            }.describe {
                summary = "List configured channels"
                parameters {
                    query("enabledOnly") { description = "Filter to enabled channels only; defaults to true" }
                }
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
                val req = call.receive<CreateChannelRequest>()
                if (req.brand.isBlank() || req.displayName.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val channel = channelRepository.create(
                    brand = req.brand.trim(),
                    config = req.config,
                    displayName = req.displayName.trim(),
                )
                call.respond(HttpStatusCode.Created, channel.toResponse(providerRegistry))
            }.describe {
                summary = "Create channel"
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
                val existing = channelRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val req = call.receive<UpdateChannelRequest>()

                val mergedConfig = req.config?.let { newConfig ->
                    val schema = providerRegistry.getOrNull(existing.brand)?.configSchema
                    if (schema == null || existing.config == null) newConfig
                    else buildJsonObject {
                        for ((key, value) in newConfig) {
                            val field = schema[key] ?: schema.sections.firstNotNullOfOrNull { it[key] }
                            val isMasked = field is ConfigField.Text.Password &&
                                value is JsonPrimitive && value.content == "***"
                            put(key, if (isMasked) (existing.config[key] ?: value) else value)
                        }
                    }
                }

                val updated = channelRepository.update(
                    id = id,
                    config = mergedConfig,
                    displayName = req.displayName,
                    isEnabled = req.isEnabled,
                ) ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, updated.toResponse(providerRegistry))
            }.describe {
                summary = "Update channel config"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Channels") }
    }

}
