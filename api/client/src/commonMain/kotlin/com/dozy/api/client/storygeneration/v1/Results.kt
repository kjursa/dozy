package com.dozy.api.client.storygeneration.v1

import com.dozy.api.client.TechnicalError as SharedTechnicalError
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.GenerationStatusError

sealed interface GenerationStartResult {
    data class Success(val response: GenerationAcceptedDto) : GenerationStartResult
    data class ApiError(val error: GenerationStartError) : GenerationStartResult
    data class TechnicalError(val error: SharedTechnicalError) : GenerationStartResult
}

sealed interface GenerationStatusResult {
    data class Success(val response: GenerationStatusDto) : GenerationStatusResult
    data class ApiError(val error: GenerationStatusError) : GenerationStatusResult
    data class TechnicalError(val error: SharedTechnicalError) : GenerationStatusResult
}

sealed interface GenerationRetryResult {
    data class Success(val response: GenerationAcceptedDto) : GenerationRetryResult
    data class ApiError(val error: GenerationRetryError) : GenerationRetryResult
    data class TechnicalError(val error: SharedTechnicalError) : GenerationRetryResult
}
