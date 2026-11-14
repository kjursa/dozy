package com.dozy.api.client.place.v1

import com.dozy.api.client.InvalidRequestException
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.place.PlaceField

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
    val fieldValue = field ?: throw InvalidResponseException(status)
    val reasonValue = reason ?: throw InvalidResponseException(status)
    val parsedField = PlaceField.fromValue(fieldValue) ?: return PlacesSaveError.Unknown(status, this)
    val parsedReason = ValidationReason.fromValue(reasonValue) ?: return PlacesSaveError.Unknown(status, this)
    if (parsedReason !in parsedField.reasons) return PlacesSaveError.Unknown(status, this)
    return PlacesSaveError.Validation(parsedField, parsedReason)
}
