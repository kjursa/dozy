package com.dozy.mobile

import com.dozy.api.client.ApiResult
import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.place.v1.PlacesClient
import com.dozy.api.client.place.v1.PlacesListError
import com.dozy.api.client.place.v1.PlacesSaveError
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient

internal fun placesClient(
    http: HttpClient,
    config: ApiConfig,
) = DozyClient(
    http,
    DozyClientConfig(config.baseUrl),
    InstallationIdProvider { "60c55c02-8d8b-46c2-a0cb-9a2d241eeea0" },
).places.v1

internal fun <T, E> apiValue(result: ApiResult<T, E>): T =
    when (result) {
        is ApiResult.Success -> result.value
        is ApiResult.Failure -> error("Expected success, received ${result.error}")
    }

internal fun <T, E> apiFailure(result: ApiResult<T, E>): E =
    when (result) {
        is ApiResult.Success -> error("Expected failure")
        is ApiResult.Failure -> result.error
    }

internal fun fakePlacesClient(list: suspend () -> ApiResult<PlacesResponse, PlacesListError>): com.dozy.api.client.place.v1.PlacesClient =
    object : com.dozy.api.client.place.v1.PlacesClient {
        override suspend fun list() = list.invoke()

        override suspend fun save(
            id: String,
            request: SavePlaceRequest,
        ): ApiResult<Unit, PlacesSaveError> = ApiResult.Failure(PlacesSaveError.Unavailable)
    }
