package com.dozy.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.EntityTagVersion
import io.ktor.http.content.VersionCheckResult
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.HexFormat

internal val apiJson = Json { encodeDefaults = true }

internal suspend inline fun <reified T> ApplicationCall.respondConditionalJson(value: T) {
    respondConditionalJsonBody(apiJson.encodeToString(value))
}

internal suspend fun ApplicationCall.respondConditionalJsonBody(body: String) {
    val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.encodeToByteArray()))
    val etag = EntityTagVersion(digest, weak = true)
    response.header(HttpHeaders.ETag, "W/\"$digest\"")
    response.header(HttpHeaders.CacheControl, "private, no-cache")
    val matches =
        request.headers.getAll(HttpHeaders.IfNoneMatch).orEmpty().any { header ->
            val tags = runCatching { EntityTagVersion.parse(header) }.getOrDefault(emptyList())
            header.trim() == "*" || etag.noneMatch(tags) == VersionCheckResult.NOT_MODIFIED
        }
    if (matches) {
        respond(HttpStatusCode.NotModified)
    } else {
        respondText(body, ContentType.Application.Json)
    }
}
