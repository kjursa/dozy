package com.dozy.mobile.feature.story.data.api

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
