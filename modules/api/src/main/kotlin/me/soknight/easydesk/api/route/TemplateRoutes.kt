@file:OptIn(ExperimentalKtorApi::class, ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.io.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.toAttachmentResponse
import me.soknight.easydesk.api.response.toResponse
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.storage.data.domain.Attachment
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import me.soknight.easydesk.service.templates.data.domain.ReplyTemplateAttachment
import me.soknight.easydesk.service.templates.data.dto.ReplyTemplateAttachmentDto
import me.soknight.easydesk.service.templates.data.repository.ReplyTemplateRepository
import org.koin.core.annotation.Single

@Serializable
data class CreateTemplateRequest(
    val content: String? = null,
    @SerialName("human_name") val humanName: String,
)

@Serializable
data class ReorderAttachmentsRequest(@SerialName("attachment_ids") val attachmentIds: List<Long>)

@Serializable
data class UpdateTemplateRequest(
    val content: String? = null,
    @SerialName("human_name") val humanName: String,
)

@Single
class TemplateRoutes(
    private val authenticator: ApiAuthenticator,
    private val storageService: AttachmentStorageService,
    private val templateRepository: ReplyTemplateRepository,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/templates") {

            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                call.respond(HttpStatusCode.OK, templateRepository.findAll().map { it.toResponse() })
            }.describe {
                summary = "List reply templates"
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
                val template = templateRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, template.toResponse())
            }.describe {
                summary = "Get reply template by ID"
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
                val req = call.receive<CreateTemplateRequest>()
                if (req.humanName.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val template = templateRepository.create(
                    name = req.humanName.trim(),
                    content = req.content,
                    createdBy = principal.agent.identifier,
                    attachments = emptyList(),
                )
                call.respond(HttpStatusCode.Created, template.toResponse())
            }.describe {
                summary = "Create reply template"
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
                val req = call.receive<UpdateTemplateRequest>()
                if (req.humanName.isBlank()) {
                    return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                }
                val existing = templateRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val updated = templateRepository.update(
                    id = id,
                    name = req.humanName.trim(),
                    content = req.content,
                    attachments = existing.attachments.filterIsInstance<ReplyTemplateAttachment>().map { it.toDto() },
                ) ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                call.respond(HttpStatusCode.OK, updated.toResponse())
            }.describe {
                summary = "Update reply template"
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
                if (!templateRepository.delete(id)) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Delete reply template"
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            post("{id}/attachments") {
                val principal = authenticator.authenticate(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@post call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val template = templateRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)

                val existing = template.attachments.filterIsInstance<ReplyTemplateAttachment>()
                if (existing.size >= ReplyTemplateAttachment.MAX_PER_TEMPLATE) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ServerErrorDto.UnprocessableEntity)
                }

                var kind: Attachment.Kind? = null
                var fileName: String? = null
                var contentType: ContentType? = null
                var fileBytes: ByteArray? = null

                call.receiveMultipart().forEachPart { part ->
                    when {
                        part is PartData.FormItem && part.name == "kind" ->
                            kind = Attachment.Kind.entries.firstOrNull { it.key == part.value }
                        part is PartData.FileItem && part.name == "file" -> {
                            fileName = part.originalFileName ?: "attachment"
                            contentType = part.contentType ?: ContentType.Application.OctetStream
                            fileBytes = part.provider().readBuffer().readByteArray()
                        }
                    }
                    part.dispose()
                }

                val resolvedFileName = fileName
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val resolvedBytes = fileBytes
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val resolvedKind = kind ?: Attachment.Kind.DOCUMENT
                val resolvedContentType = contentType ?: ContentType.Application.OctetStream

                val source = Buffer().also { it.write(resolvedBytes) }
                val storagePath = storageService.store(source, resolvedFileName, resolvedKind)

                val newDto = ReplyTemplateAttachmentDto(
                    contentType = resolvedContentType,
                    fileName = resolvedFileName,
                    fileSize = resolvedBytes.size.toLong(),
                    kind = resolvedKind,
                    storagePath = storagePath,
                )

                val updated = templateRepository.update(
                    id = id,
                    name = template.humanName,
                    content = template.content,
                    attachments = existing.map { it.toDto() } + newDto,
                ) ?: return@post call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)

                val newAttachment = updated.attachments.last()
                call.respond(HttpStatusCode.Created, newAttachment.toAttachmentResponse())
            }.describe {
                summary = "Upload template attachment"
                responses {
                    HttpStatusCode.Created { description = "Created" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                    HttpStatusCode.UnprocessableEntity { description = "Unprocessable entity" }
                }
            }

            get("{id}/attachments/{aid}/content") {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val aid = call.parameters["aid"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val template = templateRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val attachment = template.attachments.filterIsInstance<ReplyTemplateAttachment>()
                    .firstOrNull { it.attachmentId == aid }
                    ?: return@get call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val bytes = storageService.openSource(attachment.storagePath).readByteArray()
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"${attachment.fileName}\"",
                )
                call.respondBytes(bytes, attachment.contentType)
            }.describe {
                summary = "Download template attachment"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            put("{id}/attachments/order") {
                val principal = authenticator.authenticate(call)
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@put call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val template = templateRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                val req = call.receive<ReorderAttachmentsRequest>()

                val existing = template.attachments.filterIsInstance<ReplyTemplateAttachment>()
                val existingById = existing.associateBy { it.attachmentId }
                val requestedIds = req.attachmentIds.toSet()
                val reordered = req.attachmentIds.mapNotNull { existingById[it] } +
                    existing.filter { it.attachmentId !in requestedIds }

                val updated = templateRepository.update(
                    id = id,
                    name = template.humanName,
                    content = template.content,
                    attachments = reordered.map { it.toDto() },
                ) ?: return@put call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)

                call.respond(HttpStatusCode.OK, updated.attachments.map { it.toAttachmentResponse() })
            }.describe {
                summary = "Reorder template attachments"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

            delete("{id}/attachments/{aid}") {
                val principal = authenticator.authenticate(call)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                if (!principal.isAdmin) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ServerErrorDto.Forbidden)
                }
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val aid = call.parameters["aid"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ServerErrorDto.BadRequest)
                val template = templateRepository.findById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)

                val existing = template.attachments.filterIsInstance<ReplyTemplateAttachment>()
                if (existing.none { it.attachmentId == aid }) {
                    return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
                }

                templateRepository.update(
                    id = id,
                    name = template.humanName,
                    content = template.content,
                    attachments = existing.filter { it.attachmentId != aid }.map { it.toDto() },
                ) ?: return@delete call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)

                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Delete template attachment"
                responses {
                    HttpStatusCode.NoContent { description = "No content" }
                    HttpStatusCode.BadRequest { description = "Bad request" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                    HttpStatusCode.Forbidden { description = "Forbidden" }
                    HttpStatusCode.NotFound { description = "Not found" }
                }
            }

        }.describe { tag("Templates") }
    }

}

private fun ReplyTemplateAttachment.toDto() = ReplyTemplateAttachmentDto(
    attributes = attributes,
    contentType = contentType,
    fileName = fileName,
    fileSize = fileSize,
    kind = attachmentKind,
    storagePath = storagePath,
)
