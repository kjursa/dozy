package com.dozy.api.client.character.v1

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.CharacterField

sealed interface CharactersListError {
    data object Unavailable : CharactersListError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : CharactersListError
}

sealed interface CharactersSaveError {
    data class Validation(val field: CharacterField, val reason: ValidationReason) : CharactersSaveError
    data object ReferenceUnavailable : CharactersSaveError
    data object Conflict : CharactersSaveError
    data object Unavailable : CharactersSaveError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : CharactersSaveError
}

sealed interface CharactersDeleteError {
    data object Unavailable : CharactersDeleteError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : CharactersDeleteError
}
