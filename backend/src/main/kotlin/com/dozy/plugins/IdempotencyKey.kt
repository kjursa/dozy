package com.dozy.plugins

import com.dozy.domain.id.IdempotencyKey
import io.ktor.server.application.ApplicationCall

internal inline fun ApplicationCall.idempotencyKey(onInvalid: () -> Nothing): IdempotencyKey? {
    val values = request.headers.getAll("Idempotency-Key")
    if (values == null) return null
    return IdempotencyKey.parse(values.singleOrNull())
        ?: onInvalid()
}
