package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiBusinessError
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
    val code: ApiErrorCode,
    val field: String? = null,
    val reason: String? = null,
) : RuntimeException()

class BusinessApiException(val status: HttpStatusCode, val error: ApiBusinessError) : RuntimeException()

fun Application.configureErrors() {
    install(StatusPages) {
        exception<BusinessApiException> { call, failure ->
            call.respond<ApiBusinessError>(failure.status, failure.error)
        }
        exception<ApiException> { call, failure ->
            call.respond(
                failure.status,
                ApiErrorResponse(failure.code.value, failure.field, failure.reason),
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
