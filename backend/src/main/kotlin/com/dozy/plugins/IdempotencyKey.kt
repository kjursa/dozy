package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.domain.id.IdempotencyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

internal fun ApplicationCall.idempotencyKey(): IdempotencyKey? {
    val values = request.headers.getAll("Idempotency-Key")
    if (values == null) return null
    return IdempotencyKey.parse(values.singleOrNull())
        ?: throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_REQUEST)
}
