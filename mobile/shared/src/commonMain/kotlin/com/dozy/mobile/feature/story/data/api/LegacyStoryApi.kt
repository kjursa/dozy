package com.dozy.mobile.feature.story.data.api

import com.dozy.mobile.core.config.ApiConfig
import com.kjursa.foundation.coroutines.runCatchingSuspend
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException
import kotlinx.serialization.Serializable

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

interface LegacyStoryApi {
    suspend fun stories(cursor: String?): StoryPageDto?

    suspend fun retry(
        id: String,
        key: String,
    ): GenerationAcceptedDto?
}

class KtorLegacyStoryApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : LegacyStoryApi {
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
}
