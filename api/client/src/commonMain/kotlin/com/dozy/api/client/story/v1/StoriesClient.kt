package com.dozy.api.client.story.v1

import com.dozy.api.contracts.v1.story.ListStoriesRequest

interface StoriesClient {
    suspend fun list(request: ListStoriesRequest): StoryListResult

    suspend fun sync(after: Long): StorySyncResult

    suspend fun read(storyId: String): StoryReadResult
}
