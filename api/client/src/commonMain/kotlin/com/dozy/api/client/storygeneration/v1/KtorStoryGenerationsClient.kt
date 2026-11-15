package com.dozy.api.client.storygeneration.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.client.storyError
import com.dozy.api.client.storyJson
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.GenerationField
import com.dozy.api.contracts.v1.story.GenerationStage
import com.dozy.api.contracts.v1.story.GenerationFailure
import com.dozy.api.contracts.v1.story.toRequestBody
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.appendPathSegments

internal class KtorStoryGenerationsClient(
    private val http: HttpClient,
    private val config: DozyClientConfig,
    private val installations: InstallationIdProvider,
) : StoryGenerationsClient {
    override suspend fun start(idempotencyKey: String, request: GenerationRequestDto): ApiResult<GenerationAcceptedDto, GenerationStartError> {
        val response = http.post(config.endpoint(ApiVersion.V1, "story-generations")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(request.toRequestBody().toString())
        }
        if (response.status.value == 202) {
            val result = storyJson.decodeFromString<GenerationAcceptedDto>(response.bodyAsText())
            require(result.jobId.matches(Regex("[A-Za-z0-9_-]{1,128}")) && GenerationStage.entries.any { it.name == result.status })
            return ApiResult.Success(result)
        }
        val error = response.storyError()
        val mapped = when (ApiErrorCode.fromValue(error.code)) {
            ApiErrorCode.IDEMPOTENCY_CONFLICT -> GenerationStartError.IdempotencyConflict
            ApiErrorCode.ACTIVE_JOB_EXISTS -> GenerationStartError.ActiveJobExists
            ApiErrorCode.REFERENCE_UNAVAILABLE -> GenerationStartError.ReferenceUnavailable
            ApiErrorCode.UNAVAILABLE -> GenerationStartError.Unavailable
            ApiErrorCode.VALIDATION_FAILED -> {
                if (error.field == null || error.reason == null) throw InvalidResponseException(response.status.value)
                val field = GenerationField.fromValue(error.field!!)
                val reason = ValidationReason.fromValue(error.reason!!)
                if (field != null && reason in field.reasons) GenerationStartError.Validation(field, reason!!)
                else GenerationStartError.Unknown(response.status.value, error)
            }
            else -> GenerationStartError.Unknown(response.status.value, error)
        }
        return ApiResult.Failure(mapped)
    }

    override suspend fun retry(jobId: String, idempotencyKey: String): ApiResult<GenerationAcceptedDto, GenerationRetryError> {
        val response = http.post(config.endpoint(ApiVersion.V1, "story-generations")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            header("Idempotency-Key", idempotencyKey)
            url { appendPathSegments(jobId, "retry", encodeSlash = true) }
        }
        if (response.status.value == 202) {
            val result = storyJson.decodeFromString<GenerationAcceptedDto>(response.bodyAsText())
            require(result.jobId == jobId && GenerationStage.entries.any { it.name == result.status })
            return ApiResult.Success(result)
        }
        val error = response.storyError()
        return ApiResult.Failure(when (ApiErrorCode.fromValue(error.code)) {
            ApiErrorCode.JOB_NOT_FOUND -> GenerationRetryError.JobNotFound
            ApiErrorCode.JOB_NOT_FAILED -> GenerationRetryError.JobNotFailed
            ApiErrorCode.ACTIVE_JOB_EXISTS -> GenerationRetryError.ActiveJobExists
            ApiErrorCode.IDEMPOTENCY_CONFLICT -> GenerationRetryError.IdempotencyConflict
            ApiErrorCode.UNAVAILABLE -> GenerationRetryError.Unavailable
            else -> GenerationRetryError.Unknown(response.status.value, error)
        })
    }

    override suspend fun status(jobId: String): ApiResult<GenerationStatusDto, GenerationStatusError> {
        val response = http.get(config.endpoint(ApiVersion.V1, "story-generations")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(jobId, encodeSlash = true) }
        }
        if (response.status.value == 200) {
            val result = storyJson.decodeFromString<GenerationStatusDto>(response.bodyAsText())
            require(result.jobId == jobId && GenerationStage.entries.any { it.name == result.status })
            require((result.status == "READY") == (result.storyId != null))
            require(result.storyId == null || result.storyId!!.matches(Regex("[A-Za-z0-9_-]{1,128}")))
            require((result.status == "FAILED") == (result.errorCode != null))
            require(result.errorCode == null || GenerationFailure.entries.any { it.name == result.errorCode })
            return ApiResult.Success(result)
        }
        val error = response.storyError()
        return ApiResult.Failure(when (ApiErrorCode.fromValue(error.code)) {
            ApiErrorCode.JOB_NOT_FOUND -> GenerationStatusError.NotFound
            ApiErrorCode.UNAVAILABLE -> GenerationStatusError.Unavailable
            else -> GenerationStatusError.Unknown(response.status.value, error)
        })
    }
}
