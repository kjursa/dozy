package com.dozy.api.client.story.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
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
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments

internal class KtorStoriesClient(
    private val http: HttpClient,
    private val config: DozyClientConfig,
    private val installations: InstallationIdProvider,
) : StoriesClient {
    override suspend fun list(request: ListStoriesRequest): StoryListResult = communicate(StoryListResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "stories")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            parameter("limit", request.limit)
            request.cursor?.let { parameter("cursor", it) }
        }
        if (response.status.value in 200..299) {
            val page = clientJson.decodeFromString<StoryPageDto>(response.bodyAsText())
            require(page.stories.size <= request.limit && page.stories.distinctBy { it.id.lowercase() }.size == page.stories.size)
            require(page.nextCursor?.let { it.isNotBlank() && page.stories.isNotEmpty() } ?: true)
            page.stories.forEach {
                require(it.id.isNotBlank() && it.title.isNotBlank() && it.readingMinutes >= 1 && it.createdAt >= 0)
                val cover = Url(it.coverUrl)
                val base = config.baseUrl?.let(::Url)
                require(cover.host.isNotBlank() && (cover.protocol == URLProtocol.HTTPS ||
                    cover.protocol == URLProtocol.HTTP && base?.protocol == URLProtocol.HTTP && cover.host == base.host && cover.port == base.port))
            }
            return@communicate StoryListResult.Success(page)
        }
        response.businessFailure(StoryListError.serializer(), StoryListResult::ApiError, StoryListResult::TechnicalError)
    }

    override suspend fun read(storyId: String): StoryReadResult = communicate(StoryReadResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "stories")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(storyId, encodeSlash = true) }
        }
        if (response.status.value in 200..299) {
            val story = clientJson.decodeFromString<StoryDto>(response.bodyAsText())
            require(story.id == storyId && story.title.isNotBlank() && story.text.isNotBlank())
            val cover = Url(story.coverUrl)
            val base = config.baseUrl?.let(::Url)
            require(cover.host.isNotBlank() && (cover.protocol == URLProtocol.HTTPS ||
                cover.protocol == URLProtocol.HTTP && base?.protocol == URLProtocol.HTTP && cover.host == base.host && cover.port == base.port))
            require(story.createdAt >= 0)
            val configuration = story.configuration
            require(configuration.cast.isNotEmpty() && configuration.cast.distinctBy { it.id.lowercase() }.size == configuration.cast.size)
            require(GenerationMode.entries.any { it.name == configuration.mode } && StoryLength.entries.any { it.name == configuration.length })
            require(configuration.cast.all { it.id.isNotBlank() && it.name.isNotBlank() && it.personality.isNotBlank() && it.appearance.isNotBlank() && CharacterType.entries.any { type -> type.name == it.type } })
            configuration.place?.let { require(it.id.isNotBlank() && it.name.isNotBlank() && it.description.isNotBlank() && it.origin in setOf("USER", "SYSTEM")) }
            return@communicate StoryReadResult.Success(story)
        }
        response.businessFailure(StoryReadError.serializer(), StoryReadResult::ApiError, StoryReadResult::TechnicalError)
    }

}
