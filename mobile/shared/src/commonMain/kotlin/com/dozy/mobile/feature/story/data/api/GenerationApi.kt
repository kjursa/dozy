package com.dozy.mobile.feature.story.data.api

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.kjursa.foundation.coroutines.runCatchingSuspend
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.serialization.Serializable

@Serializable
data class GenerationRequestDto(
    val cast: List<CharacterDto>,
    val mode: String,
    val length: String,
    val place: PlaceDto? = null,
    val idea: String? = null,
    val ending: String? = null,
)

@Serializable
data class GenerationAcceptedDto(
    val jobId: String,
    val status: String,
)

@Serializable
data class GenerationStatusDto(
    val jobId: String,
    val status: String,
    val storyId: String? = null,
    val errorCode: String? = null,
)

@Serializable
data class StoryDto(
    val id: String,
    val title: String,
    val text: String,
    val coverUrl: String,
    val configuration: GenerationRequestDto,
    val createdAt: Long = 0,
)

class StoryNotFoundException : Exception()

@Serializable
data class StorySummaryDto(
    val id: String,
    val title: String,
    val coverUrl: String,
    val readingMinutes: Int,
    val createdAt: Long,
)

@Serializable
data class StoryPageDto(
    val stories: List<StorySummaryDto>,
    val nextCursor: String? = null,
)

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

class KtorGenerationApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : GenerationApi {
    override suspend fun stories(cursor: String?): StoryPageDto? {
        val base = config.apiBaseUrl ?: return null
        val response =
            runCatchingSuspend {
                client.get("${base.trimEnd('/')}/stories") {
                    parameter("limit", 100)
                    cursor?.let { parameter("cursor", it) }
                }
            }.getOrElse { if (it is IOException) return null else throw it }
        check(response.status == HttpStatusCode.OK)
        return response.body()
    }

    override suspend fun status(id: String): GenerationStatusDto? {
        val base = config.apiBaseUrl ?: return null
        require(validId(id))
        val response = client.get("${base.trimEnd('/')}/story-generations/$id")
        check(response.status == HttpStatusCode.OK)
        return response.body<GenerationStatusDto>().also {
            require(it.jobId == id)
            require(it.status in setOf("QUEUED", "CREATING_TEXT", "CREATING_COVER", "READY", "FAILED"))
            require((it.status == "READY") == (it.storyId != null))
            it.storyId?.let { storyId -> require(validId(storyId)) }
            require((it.status == "FAILED") == (it.errorCode != null))
            it.errorCode?.let { code -> require(code in setOf("CONTENT_REJECTED", "GENERATION_FAILED")) }
        }
    }

    override suspend fun story(id: String): StoryDto? {
        val base = config.apiBaseUrl ?: return null
        require(validId(id))
        val response = client.get("${base.trimEnd('/')}/stories/$id")
        if (response.status == HttpStatusCode.NotFound) throw StoryNotFoundException()
        check(response.status == HttpStatusCode.OK)
        return response.body<StoryDto>().also {
            require(it.id == id && it.title.isNotBlank() && it.text.isNotBlank())
            require(it.coverUrl.startsWith("https://") && it.coverUrl.length > 8)
        }
    }

    override suspend fun retry(
        id: String,
        key: String,
    ): GenerationAcceptedDto? {
        val base = config.apiBaseUrl ?: return null
        require(validId(id))
        val response =
            client.post("${base.trimEnd('/')}/story-generations/$id/retry") {
                header("Idempotency-Key", key)
            }
        check(response.status == HttpStatusCode.Accepted)
        return response.body<GenerationAcceptedDto>().also {
            require(it.jobId == id && it.status in setOf("QUEUED", "CREATING_TEXT", "CREATING_COVER", "READY", "FAILED"))
        }
    }

    private fun validId(id: String) = id.matches(Regex("[A-Za-z0-9_-]{1,128}"))

    override suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto? {
        val base = config.apiBaseUrl ?: return null
        val response =
            client.post("${base.trimEnd('/')}/story-generations") {
                header("Idempotency-Key", key)
                contentType(ContentType.Application.Json)
                setBody(request.toRequestBody())
            }
        check(response.status == HttpStatusCode.Accepted)
        return response.body<GenerationAcceptedDto>().also {
            require(it.jobId.matches(Regex("[A-Za-z0-9_-]{1,128}")))
            require(it.status in setOf("QUEUED", "CREATING_TEXT", "CREATING_COVER", "READY", "FAILED"))
        }
    }
}
