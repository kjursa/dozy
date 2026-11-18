package com.dozy.features.asset.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.plugins.ownerPartition
import com.dozy.plugins.ApiException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.assetV1Routes(repository: AssetRepository) {
    get("/assets/{id}") {
        val bytes = repository.read(call.ownerPartition(), call.parameters["id"].orEmpty())
            ?: throw ApiException(HttpStatusCode.NotFound, ApiErrorCode.ASSET_NOT_FOUND)
        call.response.headers.append(HttpHeaders.CacheControl, "private, no-store")
        call.respondBytes(bytes, ContentType.Image.PNG)
    }
}
