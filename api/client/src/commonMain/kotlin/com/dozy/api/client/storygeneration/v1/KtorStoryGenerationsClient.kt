package com.dozy.api.client.storygeneration.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.businessFailure
import com.dozy.api.client.clientJson
import com.dozy.api.client.communicate
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationFailure
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStage
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.GenerationStatusError
import com.dozy.api.contracts.v1.story.toRequestBody
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType

internal class KtorStoryGenerationsClient(
    private val http: HttpClient,
    private val config: DozyClientConfig,
    private val installations: InstallationIdProvider,
) : StoryGenerationsClient {
    override suspend fun start(idempotencyKey: String, request: GenerationRequestDto): GenerationStartResult = communicate(GenerationStartResult::TechnicalError) {
        val response = http.post(config.endpoint(ApiVersion.V1, "story-generations")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(request.toRequestBody().toString())
        }
        if (response.status.value in 200..299) {
            val result = clientJson.decodeFromString<GenerationAcceptedDto>(response.bodyAsText())
            require(result.jobId.matches(Regex("[A-Za-z0-9_-]{1,128}")) && GenerationStage.entries.any { it.name == result.status })
            return@communicate GenerationStartResult.Success(result)
        }
        response.businessFailure(GenerationStartError.serializer(), GenerationStartResult::ApiError, GenerationStartResult::TechnicalError)
    }

    override suspend fun retry(jobId: String, idempotencyKey: String): GenerationRetryResult = communicate(GenerationRetryResult::TechnicalError) {
        val response = http.post(config.endpoint(ApiVersion.V1, "story-generations")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            header("Idempotency-Key", idempotencyKey)
            url { appendPathSegments(jobId, "retry", encodeSlash = true) }
        }
        if (response.status.value in 200..299) {
            val result = clientJson.decodeFromString<GenerationAcceptedDto>(response.bodyAsText())
            require(result.jobId == jobId && GenerationStage.entries.any { it.name == result.status })
            return@communicate GenerationRetryResult.Success(result)
        }
        response.businessFailure(GenerationRetryError.serializer(), GenerationRetryResult::ApiError, GenerationRetryResult::TechnicalError)
    }

    override suspend fun status(jobId: String): GenerationStatusResult = communicate(GenerationStatusResult::TechnicalError) {
        val response = http.get(config.endpoint(ApiVersion.V1, "story-generations")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(jobId, encodeSlash = true) }
        }
        if (response.status.value in 200..299) {
            val result = clientJson.decodeFromString<GenerationStatusDto>(response.bodyAsText())
            require(result.jobId == jobId && GenerationStage.entries.any { it.name == result.status })
            require((result.status == "READY") == (result.storyId != null))
            require(result.storyId == null || result.storyId!!.matches(Regex("[A-Za-z0-9_-]{1,128}")))
            require((result.status == "FAILED") == (result.errorCode != null))
            require(result.errorCode == null || GenerationFailure.entries.any { it.name == result.errorCode })
            return@communicate GenerationStatusResult.Success(result)
        }
        response.businessFailure(GenerationStatusError.serializer(), GenerationStatusResult::ApiError, GenerationStatusResult::TechnicalError)
    }

}
