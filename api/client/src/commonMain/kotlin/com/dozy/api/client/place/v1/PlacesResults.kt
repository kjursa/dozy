package com.dozy.api.client.place.v1

import com.dozy.api.client.TechnicalError as SharedTechnicalError
import com.dozy.api.contracts.v1.place.CreatedPlaceResponse
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.PlacesSaveError

sealed interface PlacesListResult {
    data class Success(
        val response: PlacesResponse,
        val etag: String? = null,
    ) : PlacesListResult

    data object NotModified : PlacesListResult
    data class ApiError(val error: PlacesListError) : PlacesListResult
    data class TechnicalError(val error: SharedTechnicalError) : PlacesListResult
}

sealed interface PlacesCreateResult {
    data class Success(val response: CreatedPlaceResponse) : PlacesCreateResult
    data class ApiError(val error: PlacesSaveError) : PlacesCreateResult
    data class TechnicalError(val error: SharedTechnicalError) : PlacesCreateResult
}

sealed interface PlacesSaveResult {
    data object Success : PlacesSaveResult
    data class ApiError(val error: PlacesSaveError) : PlacesSaveResult
    data class TechnicalError(val error: SharedTechnicalError) : PlacesSaveResult
}
