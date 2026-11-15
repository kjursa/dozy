package com.dozy.mobile.feature.story.data.api

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.ApiResult
import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.story.v1.StoryReadError
import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient

/** Combined test fixture exercises the shared SDK while keeping flow tests concise. */
class KtorGenerationApi(
    http: HttpClient,
    config: ApiConfig,
) : GenerationApi {
    private val sdk = DozyClient(http, DozyClientConfig(config.baseUrl), InstallationIdProvider { "60c55c02-8d8b-46c2-a0cb-9a2d241eeea0" })
    private val legacy = KtorLegacyStoryApi(http, config)

    override suspend fun stories(cursor: String?) = legacy.stories(cursor)

    override suspend fun retry(
        id: String,
        key: String,
    ) = legacy.retry(id, key)

    override suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto? =
        try {
            when (val result = sdk.storyGenerations.v1.start(key, request)) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> null
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun status(id: String): GenerationStatusDto? =
        try {
            when (val result = sdk.storyGenerations.v1.status(id)) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> error("Status unavailable")
            }
        } catch (_: ApiDisabledException) {
            null
        }

    override suspend fun story(id: String): StoryDto? =
        try {
            when (val result = sdk.stories.v1.read(id)) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure ->
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
