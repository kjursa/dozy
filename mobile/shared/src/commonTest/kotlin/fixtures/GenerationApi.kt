package com.dozy.mobile.feature.story.data.api

import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryPageDto

interface GenerationApi {
    suspend fun stories(cursor: String?): StoryPageDto? = null

    suspend fun status(id: String): GenerationStatusDto?

    suspend fun story(id: String): StoryDto?

    suspend fun retry(
        id: String,
        key: String,
    ): GenerationAcceptedDto?

    suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto?
}

class StoryNotFoundException : Exception()
