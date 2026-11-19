package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import java.util.UUID

internal fun ApplicationCall.idempotencyKey(): String? {
    val values = request.headers.getAll("Idempotency-Key")
    if (values == null) return null
    val raw = values?.singleOrNull()
    val uuid = raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (uuid == null || uuid.version() != 4 || uuid.variant() != 2 ||
        !uuid.toString().equals(raw, ignoreCase = true)
    ) {
        throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_REQUEST)
    }
    return uuid.toString()
}
