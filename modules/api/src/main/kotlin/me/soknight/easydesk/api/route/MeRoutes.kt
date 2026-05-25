@file:OptIn(ExperimentalKtorApi::class, ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.response.AgentResponse
import me.soknight.easydesk.api.response.toResponse
import me.soknight.easydesk.core.model.dto.ServerErrorDto
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.Ticket.Status
import org.koin.core.annotation.Single

@Serializable
data class MeResponse(
    val agent: AgentResponse,
    @SerialName("avg_response_minutes") val avgResponseMinutes: Double?,
    @SerialName("in_progress_ticket_count") val inProgressTicketCount: Int,
    @SerialName("open_ticket_count") val openTicketCount: Int,
    @SerialName("resolved_today") val resolvedToday: Int,
)

@Single
class MeRoutes(
    private val authenticator: ApiAuthenticator,
    private val ticketRepository: TicketRepository,
) : ServerModule {

    override fun Route.configureRoutes() {
        route("/api/v1/me") {
            get {
                val principal = authenticator.authenticate(call)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ServerErrorDto.Unauthorized)
                val assigned = ticketRepository.findByAssignedAgent(principal.agent.identifier)
                val agentId = principal.agent.identifier
                call.respond(HttpStatusCode.OK, MeResponse(
                    agent = principal.agent.toResponse(principal.telegramUsername),
                    avgResponseMinutes = ticketRepository.avgFirstResponseTimeMinutes(agentId),
                    inProgressTicketCount = assigned.count { it.status == Status.IN_PROGRESS },
                    openTicketCount = assigned.count { it.status == Status.OPEN },
                    resolvedToday = ticketRepository.resolvedTodayCount(agentId),
                ))
            }.describe {
                summary = "Get current agent"
                responses {
                    HttpStatusCode.OK { description = "OK" }
                    HttpStatusCode.Unauthorized { description = "Unauthorized" }
                }
            }
        }.describe { tag("Me") }
    }

}
