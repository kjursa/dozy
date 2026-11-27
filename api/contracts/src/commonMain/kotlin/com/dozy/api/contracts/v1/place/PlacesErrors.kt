package com.dozy.api.contracts.v1.place

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface PlacesError : ApiBusinessError

@Serializable(with = PlacesListErrorSerializer::class)
sealed interface PlacesListError : PlacesError {
    data class Unknown(
        override val code: String,
    ) : PlacesListError
}

@Serializable(with = PlacesSaveErrorSerializer::class)
sealed interface PlacesSaveError : PlacesError {
    data class InvalidField(
        val invalidField: PlaceField,
        val invalidReason: ValidationReason,
    ) : PlacesSaveError {
        init {
            require(invalidReason in invalidField.reasons)
        }

        override val code = "place.invalid.field"
        override val fieldName: String get() = invalidField.value
        override val reasonCode: String get() = invalidReason.value.lowercase()
    }

    data object Forbidden : PlacesSaveError {
        override val code = "place.not_editable"
    }

    data object NotFound : PlacesSaveError {
        override val code = "place.not_found"
    }

    data object IdempotencyConflict : PlacesSaveError {
        override val code = ApiErrorCode.IDEMPOTENCY_CONFLICT.value
    }

    data object Conflict : PlacesSaveError {
        override val code = "place.conflict"
    }

    data class Unknown(
        override val code: String,
    ) : PlacesSaveError

    companion object {
        fun invalidField(
            field: PlaceField,
            reason: ValidationReason,
        ) = InvalidField(field, reason)

        val NameRequired = InvalidField(PlaceField.NAME, ValidationReason.REQUIRED)
        val NameTooLong = InvalidField(PlaceField.NAME, ValidationReason.TOO_LONG)
        val DescriptionRequired = InvalidField(PlaceField.DESCRIPTION, ValidationReason.REQUIRED)
        val DescriptionTooLong = InvalidField(PlaceField.DESCRIPTION, ValidationReason.TOO_LONG)
        val InhabitantsTooLong = InvalidField(PlaceField.INHABITANTS, ValidationReason.TOO_LONG)
        val known: List<PlacesSaveError> =
            listOf(Forbidden, NotFound, Conflict, IdempotencyConflict) +
                PlaceField.entries.flatMap { field -> field.reasons.map { InvalidField(field, it) } }
    }
}

enum class PlaceField(
    val value: String,
    val reasons: Set<ValidationReason>,
) {
    NAME("name", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)),
    DESCRIPTION("description", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)),
    INHABITANTS("inhabitants", setOf(ValidationReason.TOO_LONG)),
}

object PlacesListErrorSerializer : KSerializer<PlacesListError> by businessErrorSerializer(emptyList(), PlacesListError::Unknown)

object PlacesSaveErrorSerializer : KSerializer<PlacesSaveError> by businessErrorSerializer(PlacesSaveError.known, PlacesSaveError::Unknown)
