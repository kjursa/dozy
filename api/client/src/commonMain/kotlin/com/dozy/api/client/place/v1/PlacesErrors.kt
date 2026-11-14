package com.dozy.api.client.place.v1

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.place.PlaceField

sealed interface PlacesListError {
    data object Unavailable : PlacesListError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : PlacesListError
}

sealed interface PlacesSaveError {
    data class Validation(val field: PlaceField, val reason: ValidationReason) : PlacesSaveError
    data object Forbidden : PlacesSaveError
    data object Conflict : PlacesSaveError
    data object Unavailable : PlacesSaveError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : PlacesSaveError
}
