package com.dozy.api.contracts.v1.place

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface PlacesError : ApiBusinessError

@Serializable(with = PlacesListErrorSerializer::class)
sealed interface PlacesListError : PlacesError {
    data class Unknown(override val code: String) : PlacesListError
}

@Serializable(with = PlacesSaveErrorSerializer::class)
sealed interface PlacesSaveError : PlacesError {
    data object NameRequired : PlacesSaveError {
        override val code = "validation.name.required"
    }
    data object NameTooLong : PlacesSaveError {
        override val code = "validation.name.too_long"
    }
    data object DescriptionRequired : PlacesSaveError {
        override val code = "validation.description.required"
    }
    data object DescriptionTooLong : PlacesSaveError {
        override val code = "validation.description.too_long"
    }
    data object InhabitantsTooLong : PlacesSaveError {
        override val code = "validation.inhabitants.too_long"
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
    data class Unknown(override val code: String) : PlacesSaveError

    companion object {
        val known: List<PlacesSaveError> =
            listOf(NameRequired, NameTooLong, DescriptionRequired, DescriptionTooLong, InhabitantsTooLong, Forbidden, NotFound, Conflict, IdempotencyConflict)
        fun fromCode(code: String): PlacesSaveError =
            known.firstOrNull { it.code == code } ?: Unknown(code)
    }
}

object PlacesListErrorSerializer : KSerializer<PlacesListError> by businessErrorSerializer(emptyList(), PlacesListError::Unknown)
object PlacesSaveErrorSerializer : KSerializer<PlacesSaveError> by businessErrorSerializer(PlacesSaveError.known, PlacesSaveError::Unknown)
