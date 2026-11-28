package com.dozy.features.asset.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.domain.id.AssetId
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.plugins.notFound
import com.dozy.plugins.ownerId
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.assetV1Routes(repository: AssetRepository) {
    get("/assets/{id}") {
        val bytes =
            AssetId.parse(call.parameters["id"])?.let { repository.read(call.ownerId(), it) }
                ?: return@get call.notFound(ApiErrorResponse(ApiErrorCode.ASSET_NOT_FOUND.value))
        call.response.headers.append(HttpHeaders.CacheControl, "private, no-store")
        call.respondBytes(bytes, ContentType.Image.PNG)
    }
}
