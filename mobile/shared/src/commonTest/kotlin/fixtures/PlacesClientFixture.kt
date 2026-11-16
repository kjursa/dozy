package com.dozy.mobile

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.place.PlacesClients
import com.dozy.api.client.place.v1.PlacesClient
import com.dozy.api.client.place.v1.PlacesListResult
import com.dozy.api.client.place.v1.PlacesSaveResult
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

internal fun apiValue(result: PlacesListResult): PlacesResponse =
    when (result) {
        is PlacesListResult.Success -> result.response
        else -> error("Expected success, received $result")
    }

internal fun apiValue(result: PlacesSaveResult) {
    check(result == PlacesSaveResult.Success)
}

internal fun apiFailure(result: PlacesListResult): Any =
    when (result) {
        is PlacesListResult.ApiError -> result.error
        is PlacesListResult.TechnicalError -> result.error
        is PlacesListResult.Success -> error("Expected failure")
    }

internal fun apiFailure(result: PlacesSaveResult): Any =
    when (result) {
        is PlacesSaveResult.ApiError -> result.error
        is PlacesSaveResult.TechnicalError -> result.error
        PlacesSaveResult.Success -> error("Expected failure")
    }

internal fun fakePlacesClient(list: suspend () -> PlacesListResult): PlacesClient =
    object : PlacesClient {
        override suspend fun list() = list.invoke()

        override suspend fun save(
            id: String,
            request: SavePlaceRequest,
        ): PlacesSaveResult = PlacesSaveResult.TechnicalError(TechnicalError.Server(503))
    }

internal fun fakePlacesClients(client: PlacesClient): PlacesClients =
    object : PlacesClients {
        override val v1 = client
    }
