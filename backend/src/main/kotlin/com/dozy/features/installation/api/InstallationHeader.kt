package com.dozy.features.installation.api

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.plugins.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import java.util.UUID

fun ApplicationCall.installationId(): InstallationId {
    val value = request.headers.getAll("X-Installation-Id")?.singleOrNull()
    val parsed = value?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (parsed == null || !parsed.toString().equals(value, ignoreCase = true)) {
        throw ApiException(HttpStatusCode.BadRequest, "INVALID_INSTALLATION_ID")
    }
    return InstallationId(parsed.toString())
}
