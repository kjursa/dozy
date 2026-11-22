package com.dozy.api.client.story.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.businessFailure
import com.dozy.api.client.clientJson
import com.dozy.api.client.communicate
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.character.CharacterType
import com.dozy.api.contracts.v1.story.GenerationMode
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryLength
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryPageDto
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.api.contracts.v1.story.StorySyncDto
import com.dozy.api.contracts.v1.story.StorySyncError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments

internal class KtorStoriesClient(
    private val http: HttpClient,
    private val config: DozyClientConfig,
) : StoriesClient {
    override suspend fun list(request: ListStoriesRequest): StoryListResult = communicate(StoryListResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "stories")) {
            expectSuccess = false
            parameter("limit", request.limit)
            request.cursor?.let { parameter("cursor", it) }
        }
        if (response.status.value in 200..299) {
            val page = clientJson.decodeFromString<StoryPageDto>(response.bodyAsText())
            require(page.stories.size <= request.limit && page.stories.distinctBy { it.id.lowercase() }.size == page.stories.size)
            require(page.nextCursor?.let { it == page.stories.lastOrNull()?.id } ?: true)
            page.stories.forEach { validateSummary(it, config) }
            return@communicate StoryListResult.Success(page)
        }
        response.businessFailure(StoryListError.serializer(), StoryListResult::ApiError, StoryListResult::TechnicalError)
    }

    override suspend fun sync(after: Long): StorySyncResult =
        communicate(StorySyncResult::TechnicalError) {
            require(after >= 0)
            val response =
                http.get(config.endpoint(ApiVersion.V1, "stories")) {
                    expectSuccess = false
                    url { appendPathSegments("sync") }
                    parameter("after", after)
                }
            if (response.status == HttpStatusCode.OK) {
                val changes = clientJson.decodeFromString<StorySyncDto>(response.bodyAsText())
                changes.upserts.forEach { validateSummary(it, config) }
                val ids = changes.upserts.map { it.id } + changes.deletedIds
                require(ids.all { it.matches(storyIdPattern) } && ids.distinctBy { it.lowercase() }.size == ids.size)
                require(if (ids.isEmpty()) changes.nextRevision == after else changes.nextRevision > after)
                return@communicate StorySyncResult.Success(changes)
            }
            response.businessFailure(StorySyncError.serializer(), StorySyncResult::ApiError, StorySyncResult::TechnicalError)
        }

    override suspend fun read(storyId: String): StoryReadResult = communicate(StoryReadResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "stories")) {
            expectSuccess = false
            url { appendPathSegments(storyId, encodeSlash = true) }
        }
        if (response.status.value in 200..299) {
            val story = clientJson.decodeFromString<StoryDto>(response.bodyAsText())
            require(story.id == storyId)
            validateStory(story, config)
            return@communicate StoryReadResult.Success(story)
        }
        response.businessFailure(StoryReadError.serializer(), StoryReadResult::ApiError, StoryReadResult::TechnicalError)
    }
}

private val storyIdPattern = Regex("[A-Za-z0-9_-]{1,128}")

private fun validateSummary(
    summary: StorySummaryDto,
    config: DozyClientConfig,
) {
    require(summary.id.isNotBlank() && summary.title.isNotBlank() && summary.readingMinutes >= 1 && summary.createdAt >= 0)
    validateCoverUrl(summary.coverUrl, config)
}

internal fun validateStory(
    story: StoryDto,
    config: DozyClientConfig,
) {
    require(story.id.matches(storyIdPattern) && story.title.isNotBlank() && story.text.isNotBlank())
    validateCoverUrl(story.coverUrl, config)
    require(story.createdAt >= 0)
    val configuration = story.configuration
    require(configuration.cast.isNotEmpty() && configuration.cast.distinctBy { it.id.lowercase() }.size == configuration.cast.size)
    require(GenerationMode.entries.any { it.name == configuration.mode } && StoryLength.entries.any { it.name == configuration.length })
    require(
        configuration.cast.all {
            it.id.isNotBlank() && it.name.isNotBlank() && it.personality.isNotBlank() && it.appearance.isNotBlank() &&
                CharacterType.entries.any { type -> type.name == it.type }
        },
    )
    configuration.place?.let {
        require(it.id.isNotBlank() && it.name.isNotBlank() && it.description.isNotBlank() && it.origin in setOf("USER", "SYSTEM"))
    }
}

private fun validateCoverUrl(
    value: String,
    config: DozyClientConfig,
) {
    val cover = Url(value)
    val base = config.baseUrl?.let(::Url)
    val localDevelopmentUrl =
        cover.protocol == URLProtocol.HTTP &&
            base?.protocol == URLProtocol.HTTP && cover.host == base.host && cover.port == base.port
    require(cover.host.isNotBlank() && (cover.protocol == URLProtocol.HTTPS || localDevelopmentUrl))
}
