package me.soknight.easydesk.core.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerErrorDto(
    @SerialName("error_code") val errorCode: String,
    @SerialName("error_message") val errorMessage: String,
) {

    companion object {

        // --- 4xx Client Errors ---

        val BadRequest = ServerErrorDto("bad_request", "The request is malformed or contains invalid parameters")
        val Forbidden = ServerErrorDto("forbidden", "You do not have permission to access this resource")
        val MethodNotAllowed = ServerErrorDto("method_not_allowed", "The request method is not supported for this resource")
        val NotFound = ServerErrorDto("not_found", "The requested resource was not found")
        val Conflict = ServerErrorDto("conflict", "The request conflicts with the current state of the resource")
        val TooManyRequests = ServerErrorDto("too_many_requests", "Too many requests, please try again later")
        val Unauthorized = ServerErrorDto("unauthorized", "Authentication is required to access this resource")
        val UnprocessableEntity = ServerErrorDto("unprocessable_entity", "The request is well-formed but contains semantic errors")

        // --- 5xx Server Errors ---

        val InternalServerError = ServerErrorDto("internal_server_error", "An unexpected error occurred on the server")
        val ServiceUnavailable = ServerErrorDto("service_unavailable", "The service is temporarily unavailable")

    }

}
