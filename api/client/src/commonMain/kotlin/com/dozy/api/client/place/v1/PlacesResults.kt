package com.dozy.api.client.place.v1

import com.dozy.api.client.TechnicalError as SharedTechnicalError
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.api.contracts.v1.place.PlacesResponse

sealed interface PlacesListResult {
    data class Success(val response: PlacesResponse) : PlacesListResult
    data class ApiError(val error: PlacesListError) : PlacesListResult
    data class TechnicalError(val error: SharedTechnicalError) : PlacesListResult
}

sealed interface PlacesSaveResult {
    data object Success : PlacesSaveResult
    data class ApiError(val error: PlacesSaveError) : PlacesSaveResult
    data class TechnicalError(val error: SharedTechnicalError) : PlacesSaveResult
}
