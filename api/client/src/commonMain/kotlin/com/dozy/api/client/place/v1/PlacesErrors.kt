package com.dozy.api.client.place.v1

import com.dozy.api.contracts.v1.ApiErrorResponse

sealed interface PlacesListError {
    data object Unavailable : PlacesListError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : PlacesListError
}

sealed interface PlacesSaveError {
    data class Validation(val field: Field, val reason: Reason) : PlacesSaveError {
        enum class Field { NAME, DESCRIPTION, INHABITANTS }
        enum class Reason { REQUIRED, TOO_LONG }
    }
    data object Forbidden : PlacesSaveError
    data object Conflict : PlacesSaveError
    data object Unavailable : PlacesSaveError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : PlacesSaveError
}
