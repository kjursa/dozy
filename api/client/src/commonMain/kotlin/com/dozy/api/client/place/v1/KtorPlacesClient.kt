package com.dozy.api.client.place.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

internal class KtorPlacesClient(
    private val http: HttpClient,
    private val config: DozyClientConfig,
    private val installations: InstallationIdProvider,
) : PlacesClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun list(): ApiResult<PlacesResponse, PlacesListError> {
        val response = http.get(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
        }
        if (response.status.value == 200) {
            return ApiResult.Success(json.decodeFromString<PlacesResponse>(response.bodyAsText()))
        }
        return ApiResult.Failure(response.apiError().toListError(response.status.value))
    }

    override suspend fun save(id: String, request: SavePlaceRequest): ApiResult<Unit, PlacesSaveError> {
        require(id.isNotBlank())
        val response = http.put(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(id, encodeSlash = true) }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        if (response.status.value in setOf(201, 204)) return ApiResult.Success(Unit)
        return ApiResult.Failure(response.apiError().toSaveError(response.status.value))
    }

    private suspend fun HttpResponse.apiError(): ApiErrorResponse {
        if (status.value !in 400..599) throw InvalidResponseException(status.value)
        return json.decodeFromString<ApiErrorResponse>(bodyAsText()).also { it.checkRequest(status.value) }
    }
}
