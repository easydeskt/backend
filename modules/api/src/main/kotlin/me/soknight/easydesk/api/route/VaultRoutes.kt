@file:OptIn(ExperimentalKtorApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.vault.domain.VaultSecret
import me.soknight.easydesk.service.vault.encryption.VaultEncryptionService
import me.soknight.easydesk.service.vault.repository.VaultSecretRepository
import org.koin.core.annotation.Single

@Single
class VaultRoutes(
    private val authenticator: ApiAuthenticator,
    private val encryptionService: VaultEncryptionService,
    private val secretRepository: VaultSecretRepository,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/vault") {

            get {
                val principal = authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@get call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                call.respond(HttpStatusCode.OK, secretRepository.findAll().map { it.toResponse() })
            }.describe {
                summary = "List vault secrets"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                }
            }

            post {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@post call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val req = call.receive<CreateVaultSecretRequest>()
                if (!NAME_PATTERN.matches(req.name) || req.name.length > NAME_MAX_LENGTH) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                if (req.value.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                if (req.description != null && req.description.length > DESCRIPTION_MAX_LENGTH) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val secret = secretRepository.create(req.name, req.description, encryptionService.encrypt(req.value))
                    ?: return@post call.respond(HttpStatusCode.Conflict, ServerErrorDto.Conflict)
                call.respond(HttpStatusCode.Created, secret.toResponse())
            }.describe {
                summary = "Create vault secret"
                responses {
                    HttpStatusCode.Created { description = "Created" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.Conflict { description = "Conflict" }
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
                val req = call.receive<UpdateVaultSecretRequest>()
                if (req.value != null && req.value.isBlank()) {
                    return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                if (req.description != null && req.description.length > DESCRIPTION_MAX_LENGTH) {
                    return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val encryptedValue = req.value?.let { encryptionService.encrypt(it) }
                val secret = secretRepository.update(id, req.description, encryptedValue)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, secret.toResponse())
            }.describe {
                summary = "Update vault secret"
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
                if (!secretRepository.delete(id)) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Delete vault secret"
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Vault") }
    }

    @Serializable
    private data class CreateVaultSecretRequest(
        @SerialName("description") val description: String? = null,
        @SerialName("name") val name: String,
        @SerialName("value") val value: String,
    )

    @Serializable
    private data class UpdateVaultSecretRequest(
        @SerialName("description") val description: String? = null,
        @SerialName("value") val value: String? = null,
    )

    private companion object {
        const val DESCRIPTION_MAX_LENGTH = 500
        const val NAME_MAX_LENGTH = 64
        val NAME_PATTERN = Regex("[A-Z][A-Z0-9_]*")
    }
}

@Serializable
internal data class VaultSecretResponse(
    @SerialName("description") val description: String?,
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("updated_at") val updatedAt: Instant,
)

private fun VaultSecret.toResponse() = VaultSecretResponse(
    description = description,
    id = id,
    name = name,
    updatedAt = updatedAt,
)
