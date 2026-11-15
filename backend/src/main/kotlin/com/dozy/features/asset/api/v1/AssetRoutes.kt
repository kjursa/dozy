package com.dozy.features.asset.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.installation.api.installationId
import com.dozy.plugins.ApiException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.assetV1Routes(repository: AssetRepository) {
    get("/assets/{id}") {
        val bytes = repository.read(call.installationId(), call.parameters["id"].orEmpty())
            ?: throw ApiException(HttpStatusCode.NotFound, ApiErrorCode.ASSET_NOT_FOUND)
        call.respondBytes(bytes, ContentType.Image.PNG)
    }
}
