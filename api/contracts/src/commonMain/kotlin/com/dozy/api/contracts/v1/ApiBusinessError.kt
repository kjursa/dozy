package com.dozy.api.contracts.v1

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ApiBusinessErrorSerializer::class)
interface ApiBusinessError {
    val code: String
    data class Unknown(override val code: String) : ApiBusinessError
}

object ApiBusinessErrorSerializer : KSerializer<ApiBusinessError> by businessErrorSerializer(emptyList(), ApiBusinessError::Unknown)

internal fun <E : ApiBusinessError> businessErrorSerializer(known: List<E>, unknown: (String) -> E): KSerializer<E> = object : KSerializer<E> {
    private val envelope = ApiErrorResponse.serializer()
    override val descriptor = envelope.descriptor

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeSerializableValue(envelope, ApiErrorResponse(value.code))
    }

    override fun deserialize(decoder: Decoder): E {
        val code = decoder.decodeSerializableValue(envelope).code
        return known.firstOrNull { it.code == code } ?: unknown(code)
    }
}
