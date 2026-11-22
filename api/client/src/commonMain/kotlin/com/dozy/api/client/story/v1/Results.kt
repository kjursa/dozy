package com.dozy.api.client.story.v1

import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryPageDto
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.api.contracts.v1.story.StorySyncDto
import com.dozy.api.contracts.v1.story.StorySyncError
import com.dozy.api.client.TechnicalError as SharedTechnicalError

sealed interface StoryListResult {
    data class Success(val response: StoryPageDto) : StoryListResult
    data class ApiError(val error: StoryListError) : StoryListResult
    data class TechnicalError(val error: SharedTechnicalError) : StoryListResult
}

sealed interface StoryReadResult {
    data class Success(val response: StoryDto) : StoryReadResult
    data class ApiError(val error: StoryReadError) : StoryReadResult
    data class TechnicalError(val error: SharedTechnicalError) : StoryReadResult
}

sealed interface StorySyncResult {
    data class Success(
        val response: StorySyncDto,
    ) : StorySyncResult

    data class ApiError(
        val error: StorySyncError,
    ) : StorySyncResult

    data class TechnicalError(
        val error: SharedTechnicalError,
    ) : StorySyncResult
}
