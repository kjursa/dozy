package com.dozy.mobile.feature.story.data.api

import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode

interface LegacyStoryApi {
    suspend fun retry(
        id: String,
        key: String,
    ): GenerationAcceptedDto?
}

class KtorLegacyStoryApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : LegacyStoryApi {
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
