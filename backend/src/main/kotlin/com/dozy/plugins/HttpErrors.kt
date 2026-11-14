package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.sql.SQLException


class ApiException(
    val status: HttpStatusCode,
    val code: String,
    val field: String? = null,
    val reason: String? = null,
) : RuntimeException()

fun Application.configureErrors() {
    install(StatusPages) {
        exception<ApiException> { call, failure ->
            call.respond(
                failure.status,
                ApiErrorResponse(failure.code, failure.field, failure.reason),
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("INVALID_REQUEST"))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("INVALID_REQUEST"))
        }
        exception<SQLException> { call, _ ->
            call.respond(HttpStatusCode.ServiceUnavailable, ApiErrorResponse("UNAVAILABLE"))
        }
    }
}
