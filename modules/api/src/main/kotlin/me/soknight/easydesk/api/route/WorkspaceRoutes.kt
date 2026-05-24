@file:OptIn(ExperimentalKtorApi::class, ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.uuid.ExperimentalUuidApi
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.WorkspaceResponse
import me.soknight.easydesk.api.service.WorkspaceService
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import org.koin.core.annotation.Single

@Single
class WorkspaceRoutes(
    private val authenticator: ApiAuthenticator,
    private val workspaceService: WorkspaceService,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/workspace") {
            get {
                authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                call.respond(HttpStatusCode.OK, WorkspaceResponse(
                    metrics = workspaceService.getMetrics(),
                    name = workspaceService.workspaceName,
                    superadminId = workspaceService.getSuperadminId(),
                    startedAt = workspaceService.startedAt,
                    version = workspaceService.version,
                ))
            }.describe {
                summary = "Get workspace info"
                description = "Returns workspace name, superadmin agent UUID, and cached ticket metrics. " +
                    "Metrics are recomputed at most once per minute."
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                }
            }
        }.describe { tag("Workspace") }
    }

}
