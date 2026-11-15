package com.dozy.api.client.story.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.contracts.v1.story.StoryDto

interface StoriesClient {
    suspend fun read(storyId: String): ApiResult<StoryDto, StoryReadError>
}
