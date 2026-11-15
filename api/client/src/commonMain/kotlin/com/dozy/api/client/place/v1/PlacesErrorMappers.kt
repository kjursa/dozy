package com.dozy.api.client.place.v1

import com.dozy.api.client.InvalidRequestException
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.place.PlaceErrorCode

internal fun ApiErrorResponse.checkRequest(status: Int) {
    if (ApiErrorCode.fromValue(code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
        throw InvalidRequestException(status, this)
    }
}

internal fun ApiErrorResponse.toSaveError(status: Int): PlacesSaveError = when (PlaceErrorCode.fromValue(code)) {
    PlaceErrorCode.NAME_REQUIRED -> PlacesSaveError.NameRequired
    PlaceErrorCode.NAME_TOO_LONG -> PlacesSaveError.NameTooLong
    PlaceErrorCode.DESCRIPTION_REQUIRED -> PlacesSaveError.DescriptionRequired
    PlaceErrorCode.DESCRIPTION_TOO_LONG -> PlacesSaveError.DescriptionTooLong
    PlaceErrorCode.INHABITANTS_TOO_LONG -> PlacesSaveError.InhabitantsTooLong
    PlaceErrorCode.FORBIDDEN -> PlacesSaveError.Forbidden
    PlaceErrorCode.CONFLICT -> PlacesSaveError.Conflict
    null -> PlacesSaveError.Unknown(status, this)
}
