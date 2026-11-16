package com.dozy.api.client.place.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.InvalidRequestException
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.client.TechnicalError
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlacesError
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.api.contracts.v1.place.PlacesSaveError
import kotlinx.serialization.KSerializer
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
import kotlinx.coroutines.CancellationException
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

    override suspend fun list(): PlacesListResult = communicate(PlacesListResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
        }
        if (response.status.value == 200) {
            PlacesListResult.Success(json.decodeFromString<PlacesResponse>(response.bodyAsText()))
        } else {
            response.failure(
                PlacesListError.serializer(),
                PlacesListResult::ApiError,
                PlacesListResult::TechnicalError,
            )
        }
    }

    override suspend fun save(id: String, request: SavePlaceRequest): PlacesSaveResult = communicate(PlacesSaveResult::TechnicalError) {
        require(id.isNotBlank())
        val response = http.put(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(id, encodeSlash = true) }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        if (response.status.value in 200..299) {
            PlacesSaveResult.Success
        } else {
            response.failure(
                PlacesSaveError.serializer(),
                PlacesSaveResult::ApiError,
                PlacesSaveResult::TechnicalError,
            )
        }
    }

    private suspend fun <E : PlacesError, R> HttpResponse.failure(serializer: KSerializer<E>, apiError: (E) -> R, technicalError: (TechnicalError) -> R): R {
        if (status.value in 500..599) return technicalError(TechnicalError.Server(status.value))
        if (status.value !in 400..499) throw InvalidResponseException(status.value)
        val error = json.decodeFromString(serializer, bodyAsText())
        if (ApiErrorCode.fromValue(error.code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
            throw InvalidRequestException(status.value, ApiErrorResponse(error.code))
        }
        return apiError(error)
    }

    private suspend fun <R> communicate(technicalError: (TechnicalError) -> R, block: suspend () -> R): R =
        try {
            block()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            technicalError(TechnicalError.Communication(failure))
        }
}
