package com.dozy.api.contracts.v1.place

import com.dozy.api.contracts.v1.ApiErrorResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

interface PlacesError {
    val code: String
}

@Serializable(with = PlacesListErrorSerializer::class)
sealed interface PlacesListError : PlacesError {
    data class Unknown(override val code: String) : PlacesListError
}

@Serializable(with = PlacesSaveErrorSerializer::class)
sealed interface PlacesSaveError : PlacesError {
    data object NameRequired : PlacesSaveError { override val code = "validation.name.required" }
    data object NameTooLong : PlacesSaveError { override val code = "validation.name.too_long" }
    data object DescriptionRequired : PlacesSaveError { override val code = "validation.description.required" }
    data object DescriptionTooLong : PlacesSaveError { override val code = "validation.description.too_long" }
    data object InhabitantsTooLong : PlacesSaveError { override val code = "validation.inhabitants.too_long" }
    data object Forbidden : PlacesSaveError { override val code = "place.not_editable" }
    data object Conflict : PlacesSaveError { override val code = "place.conflict" }
    data class Unknown(override val code: String) : PlacesSaveError

    companion object {
        val known: List<PlacesSaveError> = listOf(NameRequired, NameTooLong, DescriptionRequired, DescriptionTooLong, InhabitantsTooLong, Forbidden, Conflict)
        fun fromCode(code: String): PlacesSaveError = known.firstOrNull { it.code == code } ?: Unknown(code)
    }
}

object PlacesListErrorSerializer : KSerializer<PlacesListError> by placesErrorSerializer(PlacesListError::Unknown)
object PlacesSaveErrorSerializer : KSerializer<PlacesSaveError> by placesErrorSerializer(PlacesSaveError::fromCode)

private fun <E : PlacesError> placesErrorSerializer(fromCode: (String) -> E): KSerializer<E> = object : KSerializer<E> {
    private val envelope = ApiErrorResponse.serializer()
    override val descriptor = envelope.descriptor

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeSerializableValue(envelope, ApiErrorResponse(value.code))
    }

    override fun deserialize(decoder: Decoder): E = fromCode(decoder.decodeSerializableValue(envelope).code)
}
