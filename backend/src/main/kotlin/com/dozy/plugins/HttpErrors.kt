package com.dozy.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.sql.SQLException
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val code: String)

@Serializable
data class FieldValidationErrorResponse(val code: String, val field: String, val reason: String)

@Serializable
data class FieldLengthErrorResponse(
    val code: String,
    val field: String,
    val reason: String,
    val maxLength: Int,
)

class ApiException(
    val status: HttpStatusCode,
    val code: String,
    val field: String? = null,
    val reason: String? = null,
    val maxLength: Int? = null,
) : RuntimeException()

fun Application.configureErrors() {
    install(StatusPages) {
        exception<ApiException> { call, failure ->
            val response = when {
                failure.field == null || failure.reason == null -> ErrorResponse(failure.code)
                failure.maxLength == null -> FieldValidationErrorResponse(failure.code, failure.field, failure.reason)
                else -> FieldLengthErrorResponse(failure.code, failure.field, failure.reason, failure.maxLength)
            }
            call.respond(failure.status, response)
        }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST")) }
        exception<ContentTransformationException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST")) }
        exception<SQLException> { call, _ -> call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("UNAVAILABLE")) }
    }
}
