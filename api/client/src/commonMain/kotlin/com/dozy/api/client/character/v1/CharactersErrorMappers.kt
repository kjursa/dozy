package com.dozy.api.client.character.v1

import com.dozy.api.client.InvalidRequestException
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.CharacterField

internal fun ApiErrorResponse.checkRequest(status: Int) {
    if (ApiErrorCode.fromValue(code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
        throw InvalidRequestException(status, this)
    }
}

internal fun ApiErrorResponse.toListError(status: Int): CharactersListError =
    if (ApiErrorCode.fromValue(code) == ApiErrorCode.UNAVAILABLE) CharactersListError.Unavailable
    else CharactersListError.Unknown(status, this)

internal fun ApiErrorResponse.toSaveError(status: Int): CharactersSaveError = when (ApiErrorCode.fromValue(code)) {
    ApiErrorCode.REFERENCE_UNAVAILABLE -> CharactersSaveError.ReferenceUnavailable
    ApiErrorCode.CONFLICT -> CharactersSaveError.Conflict
    ApiErrorCode.UNAVAILABLE -> CharactersSaveError.Unavailable
    ApiErrorCode.VALIDATION_FAILED -> validationError(status)
    else -> CharactersSaveError.Unknown(status, this)
}

private fun ApiErrorResponse.validationError(status: Int): CharactersSaveError {
    val fieldValue = field ?: throw InvalidResponseException(status)
    val reasonValue = reason ?: throw InvalidResponseException(status)
    val parsedField = CharacterField.fromValue(fieldValue) ?: return CharactersSaveError.Unknown(status, this)
    val parsedReason = ValidationReason.fromValue(reasonValue) ?: return CharactersSaveError.Unknown(status, this)
    if (parsedReason !in parsedField.reasons) return CharactersSaveError.Unknown(status, this)
    return CharactersSaveError.Validation(parsedField, parsedReason)
}
