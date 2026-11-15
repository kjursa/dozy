package com.dozy.api.client.place.v1

import com.dozy.api.contracts.v1.ApiErrorResponse

sealed interface PlacesListError {
    data class Unknown(val status: Int, val error: ApiErrorResponse) : PlacesListError
}

sealed interface PlacesSaveError {
    data object NameRequired : PlacesSaveError
    data object NameTooLong : PlacesSaveError
    data object DescriptionRequired : PlacesSaveError
    data object DescriptionTooLong : PlacesSaveError
    data object InhabitantsTooLong : PlacesSaveError
    data object Forbidden : PlacesSaveError
    data object Conflict : PlacesSaveError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : PlacesSaveError
}
