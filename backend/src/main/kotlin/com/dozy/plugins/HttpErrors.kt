package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.sql.SQLException

suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    error: ApiBusinessError,
) {
    respond(
        status,
        ApiErrorResponse(error.code, error.fieldName, error.reasonCode, error.attemptsLeft),
    )
}

fun Application.configureErrors() {
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
        }
        exception<ContentTransformationException> { call, _ ->
            call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
        }
        exception<SQLException> { call, _ ->
            call.serviceUnavailable(ApiErrorResponse(ApiErrorCode.UNAVAILABLE.value))
        }
    }
}
