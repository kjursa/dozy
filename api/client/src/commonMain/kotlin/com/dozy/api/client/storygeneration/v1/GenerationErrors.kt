package com.dozy.api.client.storygeneration.v1

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.GenerationField

sealed interface GenerationStartError {
    data object IdempotencyConflict : GenerationStartError
    data object ActiveJobExists : GenerationStartError
    data object ReferenceUnavailable : GenerationStartError
    data object Unavailable : GenerationStartError
    data class Validation(val field: GenerationField, val reason: ValidationReason) : GenerationStartError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : GenerationStartError
}
sealed interface GenerationStatusError {
    data object NotFound : GenerationStatusError
    data object Unavailable : GenerationStatusError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : GenerationStatusError
}

sealed interface GenerationRetryError {
    data object JobNotFound : GenerationRetryError
    data object JobNotFailed : GenerationRetryError
    data object ActiveJobExists : GenerationRetryError
    data object IdempotencyConflict : GenerationRetryError
    data object Unavailable : GenerationRetryError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : GenerationRetryError
}
