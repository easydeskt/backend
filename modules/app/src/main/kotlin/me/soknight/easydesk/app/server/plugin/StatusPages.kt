package me.soknight.easydesk.app.server.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.model.dto.ServerErrorDto

private val logger = getLogger()

/** Installs [StatusPages] with a global exception handler. */
fun Application.configureStatusPages() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ServerErrorDto.NotFound)
        }

        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(HttpStatusCode.MethodNotAllowed, ServerErrorDto.MethodNotAllowed)
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception caused HTTP 500!", cause)
            call.respond(HttpStatusCode.InternalServerError, ServerErrorDto.InternalServerError)
        }
    }
}
