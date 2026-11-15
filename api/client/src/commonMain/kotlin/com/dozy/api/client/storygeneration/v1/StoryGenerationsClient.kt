package com.dozy.api.client.storygeneration.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto

interface StoryGenerationsClient {
    suspend fun start(idempotencyKey: String, request: GenerationRequestDto): ApiResult<GenerationAcceptedDto, GenerationStartError>
    suspend fun retry(jobId: String, idempotencyKey: String): ApiResult<GenerationAcceptedDto, GenerationRetryError>
    suspend fun status(jobId: String): ApiResult<GenerationStatusDto, GenerationStatusError>
}
