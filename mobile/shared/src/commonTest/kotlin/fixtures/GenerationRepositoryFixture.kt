package com.dozy.mobile.feature.story.data

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.story.StoriesClients
import com.dozy.api.client.story.v1.StoriesClient
import com.dozy.api.client.story.v1.StoryListResult
import com.dozy.api.client.story.v1.StoryReadResult
import com.dozy.api.client.story.v1.StorySyncResult
import com.dozy.api.client.storygeneration.StoryGenerationsClients
import com.dozy.api.client.storygeneration.v1.GenerationRetryResult
import com.dozy.api.client.storygeneration.v1.GenerationStartResult
import com.dozy.api.client.storygeneration.v1.GenerationStatusResult
import com.dozy.api.client.storygeneration.v1.StoryGenerationsClient
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.StoryNotFoundException
import com.dozy.mobile.feature.story.data.database.GenerationDao
import kotlinx.coroutines.CancellationException

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
                ): GenerationStartResult =
                    fixtureCall(GenerationStartResult::TechnicalError) {
                        api.start(idempotencyKey, request)?.let { GenerationStartResult.Success(it) }
                            ?: GenerationStartResult.TechnicalError(TechnicalError.Server(503))
                    }

                override suspend fun retry(
                    jobId: String,
                    idempotencyKey: String,
                ): GenerationRetryResult =
                    fixtureCall(GenerationRetryResult::TechnicalError) {
                        api.retry(jobId, idempotencyKey)?.let { GenerationRetryResult.Success(it) }
                            ?: GenerationRetryResult.TechnicalError(TechnicalError.Server(503))
                    }

                override suspend fun status(jobId: String): GenerationStatusResult =
                    fixtureCall(GenerationStatusResult::TechnicalError) {
                        api.status(jobId)?.let { GenerationStatusResult.Success(it) }
                            ?: GenerationStatusResult.TechnicalError(TechnicalError.Server(503))
                    }
            }
    },
    object : StoriesClients {
        override val v1 =
            object : StoriesClient {
                override suspend fun list(request: ListStoriesRequest): StoryListResult =
                    fixtureCall(StoryListResult::TechnicalError) {
                        api.stories(request.cursor)?.let { StoryListResult.Success(it) }
                            ?: StoryListResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))
                    }

                override suspend fun sync(after: Long): StorySyncResult =
                    fixtureCall(StorySyncResult::TechnicalError) {
                        api.sync(after)?.let { StorySyncResult.Success(it) }
                            ?: StorySyncResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))
                    }

                override suspend fun read(storyId: String): StoryReadResult =
                    fixtureCall(StoryReadResult::TechnicalError) {
                        try {
                            api.story(storyId)?.let { StoryReadResult.Success(it) }
                                ?: StoryReadResult.TechnicalError(TechnicalError.Server(503))
                        } catch (_: StoryNotFoundException) {
                            StoryReadResult.ApiError(StoryReadError.NotFound)
                        }
                    }
            }
    },
)

private suspend fun <R> fixtureCall(
    technical: (TechnicalError) -> R,
    block: suspend () -> R,
): R =
    try {
        block()
    } catch (
        failure: CancellationException,
    ) {
        throw failure
    } catch (failure: Exception) {
        technical(TechnicalError.Communication(failure))
    }
