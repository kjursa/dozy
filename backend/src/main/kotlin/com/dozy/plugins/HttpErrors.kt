package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiErrorCode
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
) : RuntimeException() {
    constructor(status: HttpStatusCode, code: ApiErrorCode, field: String? = null, reason: String? = null) :
        this(status, code.value, field, reason)
}

fun Application.configureErrors() {
    install(StatusPages) {
        exception<ApiException> { call, failure ->
            call.respond(
                failure.status,
                ApiErrorResponse(failure.code, failure.field, failure.reason),
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
        }
        exception<SQLException> { call, _ ->
            call.respond(HttpStatusCode.ServiceUnavailable, ApiErrorResponse(ApiErrorCode.UNAVAILABLE.value))
        }
    }
}
