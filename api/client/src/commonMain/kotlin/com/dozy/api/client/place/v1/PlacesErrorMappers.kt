package com.dozy.api.client.place.v1

import com.dozy.api.client.InvalidRequestException
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse

internal fun ApiErrorResponse.checkRequest(status: Int) {
    if (ApiErrorCode.fromValue(code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
        throw InvalidRequestException(status, this)
    }
}

internal fun ApiErrorResponse.toListError(status: Int): PlacesListError =
    if (ApiErrorCode.fromValue(code) == ApiErrorCode.UNAVAILABLE) PlacesListError.Unavailable
    else PlacesListError.Unknown(status, this)

internal fun ApiErrorResponse.toSaveError(status: Int): PlacesSaveError = when (ApiErrorCode.fromValue(code)) {
    ApiErrorCode.FORBIDDEN -> PlacesSaveError.Forbidden
    ApiErrorCode.CONFLICT -> PlacesSaveError.Conflict
    ApiErrorCode.UNAVAILABLE -> PlacesSaveError.Unavailable
    ApiErrorCode.VALIDATION_FAILED -> validationError(status)
    else -> PlacesSaveError.Unknown(status, this)
}

private fun ApiErrorResponse.validationError(status: Int): PlacesSaveError {
    if (field == null || reason == null) throw InvalidResponseException(status)
    val parsedField = when (field) {
        "name" -> PlacesSaveError.Validation.Field.NAME
        "description" -> PlacesSaveError.Validation.Field.DESCRIPTION
        "inhabitants" -> PlacesSaveError.Validation.Field.INHABITANTS
        else -> return PlacesSaveError.Unknown(status, this)
    }
    val parsedReason = when (reason) {
        "REQUIRED" -> PlacesSaveError.Validation.Reason.REQUIRED
        "TOO_LONG" -> PlacesSaveError.Validation.Reason.TOO_LONG
        else -> return PlacesSaveError.Unknown(status, this)
    }
    if (parsedField == PlacesSaveError.Validation.Field.INHABITANTS && parsedReason == PlacesSaveError.Validation.Reason.REQUIRED) {
        return PlacesSaveError.Unknown(status, this)
    }
    return PlacesSaveError.Validation(parsedField, parsedReason)
}
