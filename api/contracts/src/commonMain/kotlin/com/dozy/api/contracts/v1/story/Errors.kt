package com.dozy.api.contracts.v1.story

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.businessErrorSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = GenerationStartErrorSerializer::class)
sealed interface GenerationStartError : ApiBusinessError {
    data class Validation(val field: GenerationField, val reason: ValidationReason) : GenerationStartError {
        init { require(reason in field.reasons) }
        override val code: String get() = "validation.${this.field.value}.${reason.value.lowercase()}"
    }
    data object IdempotencyConflict : GenerationStartError { override val code = ApiErrorCode.IDEMPOTENCY_CONFLICT.value }
    data object ActiveJobExists : GenerationStartError { override val code = ApiErrorCode.ACTIVE_JOB_EXISTS.value }
    data object ReferenceUnavailable : GenerationStartError { override val code = ApiErrorCode.REFERENCE_UNAVAILABLE.value }
    data class Unknown(override val code: String) : GenerationStartError

    companion object {
        val known: List<GenerationStartError> = listOf<GenerationStartError>(IdempotencyConflict, ActiveJobExists, ReferenceUnavailable) + GenerationField.entries.flatMap { field -> field.reasons.map { Validation(field, it) } }
    }
}

object GenerationStartErrorSerializer : KSerializer<GenerationStartError> by businessErrorSerializer(GenerationStartError.known, GenerationStartError::Unknown)

@Serializable(with = GenerationStatusErrorSerializer::class)
sealed interface GenerationStatusError : ApiBusinessError {
    data object NotFound : GenerationStatusError { override val code = ApiErrorCode.JOB_NOT_FOUND.value }
    data class Unknown(override val code: String) : GenerationStatusError

    companion object {
        val known: List<GenerationStatusError> = listOf<GenerationStatusError>(NotFound)
    }
}

object GenerationStatusErrorSerializer : KSerializer<GenerationStatusError> by businessErrorSerializer(GenerationStatusError.known, GenerationStatusError::Unknown)

@Serializable(with = GenerationRetryErrorSerializer::class)
sealed interface GenerationRetryError : ApiBusinessError {
    data object JobNotFound : GenerationRetryError { override val code = ApiErrorCode.JOB_NOT_FOUND.value }
    data object JobNotFailed : GenerationRetryError { override val code = ApiErrorCode.JOB_NOT_FAILED.value }
    data object ActiveJobExists : GenerationRetryError { override val code = ApiErrorCode.ACTIVE_JOB_EXISTS.value }
    data object IdempotencyConflict : GenerationRetryError { override val code = ApiErrorCode.IDEMPOTENCY_CONFLICT.value }
    data class Unknown(override val code: String) : GenerationRetryError

    companion object {
        val known: List<GenerationRetryError> = listOf<GenerationRetryError>(JobNotFound, JobNotFailed, ActiveJobExists, IdempotencyConflict)
    }
}

object GenerationRetryErrorSerializer : KSerializer<GenerationRetryError> by businessErrorSerializer(GenerationRetryError.known, GenerationRetryError::Unknown)

@Serializable(with = StoryListErrorSerializer::class)
sealed interface StoryListError : ApiBusinessError {
    data class Validation(val field: StoryListField, val reason: ValidationReason) : StoryListError {
        init { require(reason in field.reasons) }
        override val code: String get() = "validation.${this.field.value}.${reason.value.lowercase()}"
    }
    data object InvalidCursor : StoryListError { override val code = ApiErrorCode.INVALID_CURSOR.value }
    data class Unknown(override val code: String) : StoryListError

    companion object {
        val known: List<StoryListError> = listOf<StoryListError>(InvalidCursor) + StoryListField.entries.flatMap { field -> field.reasons.map { Validation(field, it) } }
    }
}

object StoryListErrorSerializer : KSerializer<StoryListError> by businessErrorSerializer(StoryListError.known, StoryListError::Unknown)

@Serializable(with = StoryReadErrorSerializer::class)
sealed interface StoryReadError : ApiBusinessError {
    data object NotFound : StoryReadError { override val code = ApiErrorCode.STORY_NOT_FOUND.value }
    data class Unknown(override val code: String) : StoryReadError

    companion object {
        val known: List<StoryReadError> = listOf<StoryReadError>(NotFound)
    }
}

object StoryReadErrorSerializer : KSerializer<StoryReadError> by businessErrorSerializer(StoryReadError.known, StoryReadError::Unknown)

@Serializable(with = StorySyncErrorSerializer::class)
sealed interface StorySyncError : ApiBusinessError {
    data object InvalidRevision : StorySyncError {
        override val code = ApiErrorCode.INVALID_CURSOR.value
    }

    data class Unknown(
        override val code: String,
    ) : StorySyncError
}

object StorySyncErrorSerializer : KSerializer<StorySyncError> by
businessErrorSerializer(listOf(StorySyncError.InvalidRevision), StorySyncError::Unknown)
