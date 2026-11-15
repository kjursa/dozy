package com.dozy.api.client.place.v1

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.ApiResult
import com.dozy.api.client.ClientError
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.InvalidRequestException
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
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
import kotlinx.io.IOException
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

    override suspend fun list(): ApiResult<PlacesResponse, ClientError<PlacesListError>> = communicate {
        val response = http.get(config.endpoint(ApiVersion.V1, "places")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
        }
        if (response.status.value == 200) {
            return@communicate ApiResult.Success(json.decodeFromString<PlacesResponse>(response.bodyAsText()))
        }
        response.failure { PlacesListError.Unknown(response.status.value, it) }
    }

    override suspend fun save(id: String, request: SavePlaceRequest): ApiResult<Unit, ClientError<PlacesSaveError>> {
        require(id.isNotBlank())
        return communicate {
            val response = http.put(config.endpoint(ApiVersion.V1, "places")) {
                expectSuccess = false
                header("X-Installation-Id", installations.get())
                url { appendPathSegments(id, encodeSlash = true) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (response.status.value in setOf(201, 204)) return@communicate ApiResult.Success(Unit)
            response.failure { it.toSaveError(response.status.value) }
        }
    }

    private suspend fun <E> HttpResponse.failure(map: (ApiErrorResponse) -> E): ApiResult<Nothing, ClientError<E>> {
        if (status.value in 500..599) return ApiResult.Failure(ClientError.ServerError(status.value))
        if (status.value !in 400..499) throw InvalidResponseException(status.value)
        val error = json.decodeFromString<ApiErrorResponse>(bodyAsText())
        error.checkRequest(status.value)
        return ApiResult.Failure(ClientError.ApiError(map(error)))
    }

    private suspend fun <T, E> communicate(block: suspend () -> ApiResult<T, ClientError<E>>): ApiResult<T, ClientError<E>> =
        try {
            block()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            when (failure) {
                is IOException, is HttpRequestTimeoutException, is SerializationException,
                is InvalidResponseException, is InvalidRequestException, is ApiDisabledException ->
                    ApiResult.Failure(ClientError.CommunicationError(failure))
                else -> throw failure
            }
        }
}
