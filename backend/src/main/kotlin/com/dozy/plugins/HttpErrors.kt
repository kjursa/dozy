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

class ApiException(val status: HttpStatusCode, val code: String) : RuntimeException()

fun Application.configureErrors() {
    install(StatusPages) {
        exception<ApiException> { call, failure -> call.respond(failure.status, ErrorResponse(failure.code)) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST")) }
        exception<ContentTransformationException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST")) }
        exception<SQLException> { call, _ -> call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("UNAVAILABLE")) }
    }
}
