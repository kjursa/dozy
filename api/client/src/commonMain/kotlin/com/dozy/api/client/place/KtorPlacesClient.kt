package com.dozy.api.client.place

import com.dozy.api.client.DozyApiException
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlaceResponse
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
import kotlinx.serialization.SerializationException
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

    override suspend fun list(): List<PlaceResponse> {
        val response = http.get(config.endpoint("places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
        }
        response.requireStatus(200)
        return json.decodeFromString<PlacesResponse>(response.bodyAsText()).places
    }

    override suspend fun save(id: String, request: SavePlaceRequest) {
        require(id.isNotBlank())
        val response = http.put(config.endpoint("places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(id, encodeSlash = true) }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        response.requireStatus(201, 204)
    }

    private suspend fun HttpResponse.requireStatus(vararg accepted: Int) {
        if (status.value in accepted) return
        val error = try {
            json.decodeFromString<ApiErrorResponse>(bodyAsText())
        } catch (_: SerializationException) {
            null
        }
        throw DozyApiException(status.value, error)
    }
}
