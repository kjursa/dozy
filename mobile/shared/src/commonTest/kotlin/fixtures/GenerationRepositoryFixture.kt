package com.dozy.mobile.feature.story.data

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.ApiResult
import com.dozy.api.client.story.StoriesClients
import com.dozy.api.client.story.v1.StoriesClient
import com.dozy.api.client.story.v1.StoryListError
import com.dozy.api.client.story.v1.StoryReadError
import com.dozy.api.client.storygeneration.StoryGenerationsClients
import com.dozy.api.client.storygeneration.v1.GenerationRetryError
import com.dozy.api.client.storygeneration.v1.GenerationStartError
import com.dozy.api.client.storygeneration.v1.GenerationStatusError
import com.dozy.api.client.storygeneration.v1.StoryGenerationsClient
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.mobile.feature.story.data.api.GenerationAcceptedDto
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.GenerationRequestDto
import com.dozy.mobile.feature.story.data.api.GenerationStatusDto
import com.dozy.mobile.feature.story.data.api.StoryDto
import com.dozy.mobile.feature.story.data.api.StoryNotFoundException
import com.dozy.mobile.feature.story.data.api.StoryPageDto
import com.dozy.mobile.feature.story.data.database.GenerationDao

internal fun testGenerationRepository(
    dao: GenerationDao,
    api: GenerationApi,
) = DefaultGenerationRepository(
    dao,
    object : StoryGenerationsClients {
        override val v1 =
            object : StoryGenerationsClient {
                override suspend fun start(
                    idempotencyKey: String,
                    request: GenerationRequestDto,
                ): ApiResult<GenerationAcceptedDto, GenerationStartError> =
                    api.start(idempotencyKey, request)?.let { ApiResult.Success(it) } ?: ApiResult.Failure(GenerationStartError.Unavailable)

                override suspend fun retry(
                    jobId: String,
                    idempotencyKey: String,
                ): ApiResult<GenerationAcceptedDto, GenerationRetryError> =
                    api.retry(jobId, idempotencyKey)?.let { ApiResult.Success(it) } ?: ApiResult.Failure(GenerationRetryError.Unavailable)

                override suspend fun status(jobId: String): ApiResult<GenerationStatusDto, GenerationStatusError> =
                    api.status(jobId)?.let { ApiResult.Success(it) } ?: ApiResult.Failure(GenerationStatusError.Unavailable)
            }
    },
    object : StoriesClients {
        override val v1 =
            object : StoriesClient {
                override suspend fun list(request: ListStoriesRequest): ApiResult<StoryPageDto, StoryListError> =
                    api.stories(request.cursor)?.let { ApiResult.Success(it) } ?: throw ApiDisabledException()

                override suspend fun read(storyId: String): ApiResult<StoryDto, StoryReadError> =
                    try {
                        api.story(storyId)?.let { ApiResult.Success(it) } ?: ApiResult.Failure(StoryReadError.Unavailable)
                    } catch (_: StoryNotFoundException) {
                        ApiResult.Failure(StoryReadError.NotFound)
                    }
            }
    },
)
