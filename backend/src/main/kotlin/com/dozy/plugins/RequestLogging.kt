package com.dozy.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import java.util.UUID

private val requestIdFormat = Regex("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}")

internal fun Application.configureRequestLogging() {
    install(CallId) {
        retrieve { call ->
            call.request.headers[HttpHeaders.XRequestId]?.takeIf { it.length == 36 && requestIdFormat.matches(it) }
        }
        generate { UUID.randomUUID().toString() }
        verify(requestIdFormat::matches)
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        callIdMdc("requestId")
    }
}
