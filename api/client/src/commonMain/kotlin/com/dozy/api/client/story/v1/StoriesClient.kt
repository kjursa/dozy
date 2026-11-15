package com.dozy.api.client.story.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryPageDto

interface StoriesClient {
    suspend fun list(request: ListStoriesRequest): ApiResult<StoryPageDto, StoryListError>
    suspend fun read(storyId: String): ApiResult<StoryDto, StoryReadError>
}
