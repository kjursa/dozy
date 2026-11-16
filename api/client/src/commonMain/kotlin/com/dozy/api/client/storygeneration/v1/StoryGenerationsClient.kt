package com.dozy.api.client.storygeneration.v1

import com.dozy.api.contracts.v1.story.GenerationRequestDto

interface StoryGenerationsClient {
    suspend fun start(idempotencyKey: String, request: GenerationRequestDto): GenerationStartResult
    suspend fun retry(jobId: String, idempotencyKey: String): GenerationRetryResult
    suspend fun status(jobId: String): GenerationStatusResult
}
