package com.dozy.mobile.core.data.api

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.hostWithPort
import io.ktor.util.AttributeKey
import io.ktor.utils.io.readRemaining
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val BODY_LIMIT = 16_384

private data class LoggedAttempt(val id: String, val started: TimeMark)

private val attemptKey = AttributeKey<LoggedAttempt>("DozyHttpAttempt")

@OptIn(ExperimentalUuidApi::class)
internal fun httpDiagnostics(log: ((String) -> Unit)?) =
    createClientPlugin("DozyHttpDiagnostics") {
        on(SendingRequest) { request, content ->
            val requestId = Uuid.random().toString()
            request.headers.remove(HttpHeaders.XRequestId)
            request.headers.append(HttpHeaders.XRequestId, requestId)
            if (log == null) return@on

            val attempt = LoggedAttempt(requestId, TimeSource.Monotonic.markNow())
            request.attributes.put(attemptKey, attempt)
            val hasAuthorization = request.headers.contains(HttpHeaders.Authorization)
            log("-> [${attempt.id}] ${request.method.value} ${request.url.build().logAddress()} authorizationPresent=$hasAuthorization")
            if (content is TextContent && content.contentType.contentSubtype.isJsonSubtype()) {
                val body = if (request.url.build().encodedPath.contains("/auth/")) "[auth request body omitted]" else content.text.safeJson()
                logBody(log, attempt.id, "request", body)
            }
        }
        onResponse { response ->
            if (log == null) return@onResponse
            val request = response.call.request
            val attempt = request.attributes.getOrNull(attemptKey)
            val elapsed = attempt?.started?.elapsedNow()?.inWholeMilliseconds
            log("<- [${attempt?.id ?: "?"}] ${response.status.value} ${request.method.value} ${request.url.logAddress()} (${elapsed ?: "?"} ms)")
        }
    }

private fun Url.logAddress(): String = "${protocol.name}://$hostWithPort$encodedPath"

internal fun HttpClientConfig<*>.installHttpResponseBodies(log: (String) -> Unit) {
    install(ResponseObserver) {
        filter { it.response.contentType()?.contentSubtype?.isJsonSubtype() == true }
        onResponse { response ->
            val id = response.call.request.attributes.getOrNull(attemptKey)?.id ?: return@onResponse
            val bytes = response.bodyAsChannel().readRemaining(BODY_LIMIT.toLong() + 1).readByteArray()
            val body = if (bytes.size > BODY_LIMIT) "[body exceeds $BODY_LIMIT bytes]" else bytes.decodeToString().safeJson()
            logBody(log, id, "response", body)
        }
    }
}

private fun String.isJsonSubtype(): Boolean = equals("json", ignoreCase = true) || endsWith("+json", ignoreCase = true)

private fun String.safeJson(): String {
    if (isBlank()) return "[empty]"
    if (length > BODY_LIMIT) return "[body exceeds $BODY_LIMIT characters]"
    val json = runCatching { Json.parseToJsonElement(this) }.getOrNull() ?: return "[invalid JSON omitted]"
    return json.redactCredentials().toString()
}

private fun JsonElement.redactCredentials(): JsonElement =
    when (this) {
        is JsonObject -> JsonObject(mapValues { (key, value) ->
            val normalized = key.filter(Char::isLetterOrDigit).lowercase()
            if (normalized in setOf("accesstoken", "refreshtoken", "token", "password", "authorization", "email", "sessionid", "otp", "pin")) {
                JsonPrimitive("[redacted]")
            } else {
                value.redactCredentials()
            }
        })
        is JsonArray -> JsonArray(map { it.redactCredentials() })
        else -> this
    }

private fun logBody(log: (String) -> Unit, id: String, direction: String, body: String) {
    body.chunked(2_000).forEachIndexed { index, chunk ->
        log("[$id] $direction body ${index + 1}: $chunk")
    }
}
