package com.dozy.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.EntityTagVersion
import io.ktor.http.content.VersionCheckResult
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond

internal fun ApplicationCall.collectionRevalidation(): (String) -> Boolean {
    val validators = request.headers.getAll(HttpHeaders.IfNoneMatch).orEmpty()
    val tags = validators.flatMap { runCatching { EntityTagVersion.parse(it) }.getOrDefault(emptyList()) }
    val anyVersion = validators.any { it.trim() == "*" }
    return { version ->
        anyVersion || (tags.isNotEmpty() && EntityTagVersion(version, weak = true).noneMatch(tags) == VersionCheckResult.NOT_MODIFIED)
    }
}

internal suspend inline fun <reified T : Any> ApplicationCall.respondVersionedCollection(
    version: String,
    body: T?,
) {
    response.header(HttpHeaders.ETag, "W/\"$version\"")
    response.header(HttpHeaders.CacheControl, "private, no-cache")
    if (body == null) respond(HttpStatusCode.NotModified) else respond(body)
}
