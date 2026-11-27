package com.dozy.api.contracts.v1.characteravatar

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = AvatarStartErrorSerializer::class)
sealed interface AvatarStartError : ApiBusinessError {
    data class Validation(
        val invalidField: AvatarField,
        val invalidReason: ValidationReason,
    ) : AvatarStartError {
        init {
            require(invalidReason in invalidField.reasons)
        }

        override val code: String get() = "avatar.invalid.field"
        override val fieldName: String get() = invalidField.value
        override val reasonCode: String get() = invalidReason.value.lowercase()
    }

    data class Unknown(
        override val code: String,
    ) : AvatarStartError

    companion object {
        fun invalidField(
            field: AvatarField,
            reason: ValidationReason,
        ) = Validation(field, reason)

        val known: List<AvatarStartError> =
            listOf<AvatarStartError>() + AvatarField.entries.flatMap { field -> field.reasons.map { Validation(field, it) } }
    }
}

object AvatarStartErrorSerializer : KSerializer<AvatarStartError> by businessErrorSerializer(
    AvatarStartError.known,
    AvatarStartError::Unknown,
)

@Serializable(with = AvatarStatusErrorSerializer::class)
sealed interface AvatarStatusError : ApiBusinessError {
    data object NotFound : AvatarStatusError {
        override val code = ApiErrorCode.AVATAR_JOB_NOT_FOUND.value
    }

    data class Unknown(
        override val code: String,
    ) : AvatarStatusError

    companion object {
        val known: List<AvatarStatusError> = listOf<AvatarStatusError>(NotFound)
    }
}

object AvatarStatusErrorSerializer : KSerializer<AvatarStatusError> by businessErrorSerializer(
    AvatarStatusError.known,
    AvatarStatusError::Unknown,
)
