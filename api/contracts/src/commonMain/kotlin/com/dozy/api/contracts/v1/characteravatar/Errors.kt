package com.dozy.api.contracts.v1.characteravatar

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = AvatarStartErrorSerializer::class)
sealed interface AvatarStartError : ApiBusinessError {
    data class Validation(val field: AvatarField, val reason: ValidationReason) : AvatarStartError {
        init { require(reason in field.reasons) }
        override val code: String get() = "validation.${this.field.value}.${reason.value.lowercase()}"
    }
    data class Unknown(override val code: String) : AvatarStartError

    companion object {
        val known: List<AvatarStartError> = listOf<AvatarStartError>() + AvatarField.entries.flatMap { field -> field.reasons.map { Validation(field, it) } }
    }
}

object AvatarStartErrorSerializer : KSerializer<AvatarStartError> by businessErrorSerializer(AvatarStartError.known, AvatarStartError::Unknown)

@Serializable(with = AvatarStatusErrorSerializer::class)
sealed interface AvatarStatusError : ApiBusinessError {
    data object NotFound : AvatarStatusError { override val code = ApiErrorCode.AVATAR_JOB_NOT_FOUND.value }
    data class Unknown(override val code: String) : AvatarStatusError

    companion object {
        val known: List<AvatarStatusError> = listOf<AvatarStatusError>(NotFound)
    }
}

object AvatarStatusErrorSerializer : KSerializer<AvatarStatusError> by businessErrorSerializer(AvatarStatusError.known, AvatarStatusError::Unknown)
