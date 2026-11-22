package com.dozy.mobile.feature.story.data.api

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.story.v1.StoryListResult
import com.dozy.api.client.story.v1.StoryReadResult
import com.dozy.api.client.story.v1.StorySyncResult
import com.dozy.api.client.storygeneration.v1.GenerationRetryResult
import com.dozy.api.client.storygeneration.v1.GenerationStartResult
import com.dozy.api.client.storygeneration.v1.GenerationStatusResult
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient

/** Combined test fixture exercises the shared SDK while keeping flow tests concise. */
class KtorGenerationApi(
    http: HttpClient,
    config: ApiConfig,
) : GenerationApi {
    private val sdk = DozyClient(http, DozyClientConfig(config.baseUrl))

    override suspend fun stories(cursor: String?) =
        try {
            when (val result = sdk.stories.v1.list(ListStoriesRequest(cursor = cursor))) {
                is StoryListResult.Success -> result.response
                is StoryListResult.TechnicalError -> legacyTechnical(result.error)
                is StoryListResult.ApiError -> null
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun sync(after: Long) =
        try {
            when (val result = sdk.stories.v1.sync(after)) {
                is StorySyncResult.Success -> result.response
                is StorySyncResult.TechnicalError -> legacyTechnical(result.error)
                is StorySyncResult.ApiError -> error("Library unavailable")
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun retry(
        id: String,
        key: String,
    ): GenerationAcceptedDto? =
        try {
            when (val result = sdk.storyGenerations.v1.retry(id, key)) {
                is GenerationRetryResult.Success -> result.response
                is GenerationRetryResult.TechnicalError -> legacyTechnical(result.error)
                is GenerationRetryResult.ApiError -> null
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto? =
        try {
            when (val result = sdk.storyGenerations.v1.start(key, request)) {
                is GenerationStartResult.Success -> result.response
                is GenerationStartResult.TechnicalError -> legacyTechnical(result.error)
                is GenerationStartResult.ApiError -> null
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun status(id: String): GenerationStatusDto? =
        try {
            when (val result = sdk.storyGenerations.v1.status(id)) {
                is GenerationStatusResult.Success -> result.response
                is GenerationStatusResult.TechnicalError -> legacyTechnical(result.error)
                is GenerationStatusResult.ApiError -> error("Status unavailable")
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun story(id: String): StoryDto? =
        try {
            when (val result = sdk.stories.v1.read(id)) {
                is StoryReadResult.Success -> result.response
                is StoryReadResult.TechnicalError -> legacyTechnical(result.error)
                is StoryReadResult.ApiError ->
                    if (result.error ==
                        StoryReadError.NotFound
                    ) {
                        throw StoryNotFoundException()
                    } else {
                        error("Story unavailable")
                    }
            }
        } catch (_: ApiDisabledException) {
            null
        }
}

private fun legacyTechnical(error: TechnicalError): Nothing =
    when (error) {
        is TechnicalError.Server -> error("Server unavailable")
        is TechnicalError.Communication -> throw error.exception
    }
