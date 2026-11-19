package com.dozy.api.client.place.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.businessFailure
import com.dozy.api.client.communicate
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.place.CreatedPlaceResponse
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

internal class KtorPlacesClient(
    private val http: HttpClient,
    private val config: DozyClientConfig
) : PlacesClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun list(): PlacesListResult = communicate(PlacesListResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
        }
        if (response.status.value == 200) {
            PlacesListResult.Success(json.decodeFromString<PlacesResponse>(response.bodyAsText()))
        } else {
            response.businessFailure(
                PlacesListError.serializer(),
                PlacesListResult::ApiError,
                PlacesListResult::TechnicalError
            )
        }
    }

    override suspend fun create(
        request: SavePlaceRequest,
        requestKey: String?
    ): PlacesCreateResult = communicate(PlacesCreateResult::TechnicalError) {
        val response = http.post(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            requestKey?.let { header("Idempotency-Key", it) }
            setBody(json.encodeToString(request))
        }
        if (response.status == HttpStatusCode.Created) {
            val created = json.decodeFromString<CreatedPlaceResponse>(response.bodyAsText())
            require(created.id.isNotBlank())
            PlacesCreateResult.Success(created)
        } else {
            response.businessFailure(
                PlacesSaveError.serializer(),
                PlacesCreateResult::ApiError,
                PlacesCreateResult::TechnicalError
            )
        }
    }

    override suspend fun save(id: String, request: SavePlaceRequest): PlacesSaveResult =
        communicate(PlacesSaveResult::TechnicalError) {
            require(id.isNotBlank())
            val response = http.put(config.endpoint(ApiVersion.V1, "places")) {
                expectSuccess = false
                url { appendPathSegments(id, encodeSlash = true) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (response.status.value in 200..299) {
                PlacesSaveResult.Success
            } else {
                response.businessFailure(
                    PlacesSaveError.serializer(),
                    PlacesSaveResult::ApiError,
                    PlacesSaveResult::TechnicalError
                )
            }
        }
}
