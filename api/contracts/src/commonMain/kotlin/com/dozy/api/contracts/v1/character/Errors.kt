package com.dozy.api.contracts.v1.character

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = CharactersListErrorSerializer::class)
sealed interface CharactersListError : ApiBusinessError {
    data class Unknown(override val code: String) : CharactersListError

    companion object {
        val known: List<CharactersListError> = listOf<CharactersListError>()
    }
}

object CharactersListErrorSerializer : KSerializer<CharactersListError> by businessErrorSerializer(CharactersListError.known, CharactersListError::Unknown)

@Serializable(with = CharactersSaveErrorSerializer::class)
sealed interface CharactersSaveError : ApiBusinessError {
    data class Validation(val field: CharacterField, val reason: ValidationReason) : CharactersSaveError {
        init { require(reason in field.reasons) }
        override val code: String get() = "validation.${this.field.value}.${reason.value.lowercase()}"
    }
    data object ReferenceUnavailable : CharactersSaveError {
        override val code = ApiErrorCode.REFERENCE_UNAVAILABLE.value
    }
    data object NotFound : CharactersSaveError {
        override val code = ApiErrorCode.CHARACTER_NOT_FOUND.value
    }
    data object IdempotencyConflict : CharactersSaveError {
        override val code = ApiErrorCode.IDEMPOTENCY_CONFLICT.value
    }
    data object Conflict : CharactersSaveError {
        override val code = ApiErrorCode.CONFLICT.value
    }
    data class Unknown(override val code: String) : CharactersSaveError

    companion object {
        val known: List<CharactersSaveError> =
            listOf<CharactersSaveError>(
                ReferenceUnavailable,
                NotFound,
                Conflict,
                IdempotencyConflict
            ) +
                CharacterField.entries.flatMap { field ->
                    field.reasons.map { Validation(field, it) }
                }
    }
}

object CharactersSaveErrorSerializer : KSerializer<CharactersSaveError> by businessErrorSerializer(CharactersSaveError.known, CharactersSaveError::Unknown)

@Serializable(with = CharactersDeleteErrorSerializer::class)
sealed interface CharactersDeleteError : ApiBusinessError {
    data object NotFound : CharactersDeleteError { override val code = ApiErrorCode.CHARACTER_NOT_FOUND.value }
    data class Unknown(override val code: String) : CharactersDeleteError

    companion object {
        val known: List<CharactersDeleteError> = listOf<CharactersDeleteError>(NotFound)
    }
}

object CharactersDeleteErrorSerializer : KSerializer<CharactersDeleteError> by businessErrorSerializer(CharactersDeleteError.known, CharactersDeleteError::Unknown)
