package com.dozy.api.client.characteravatar.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.InvalidRequestException
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.characteravatar.AvatarField
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobState
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class KtorCharacterAvatarsClient(
    private val http: HttpClient,
    private val config: DozyClientConfig,
    private val installations: InstallationIdProvider,
) : CharacterAvatarsClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun start(request: AvatarRequestDto): ApiResult<AvatarJobDto, AvatarStartError> {
        val response = http.post(config.endpoint(ApiVersion.V1, "character-avatars")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        if (response.status.value == 202) return ApiResult.Success(response.job())
        val error = response.error()
        val mapped = when (ApiErrorCode.fromValue(error.code)) {
            ApiErrorCode.UNAVAILABLE -> AvatarStartError.Unavailable
            ApiErrorCode.VALIDATION_FAILED -> {
                if (error.field == null || error.reason == null) throw InvalidResponseException(response.status.value)
                val field = AvatarField.fromValue(requireNotNull(error.field))
                val reason = ValidationReason.fromValue(requireNotNull(error.reason))
                if (field != null && reason != null && reason in field.reasons) {
                    AvatarStartError.Validation(field, reason)
                } else AvatarStartError.Unknown(response.status.value, error)
            }
            else -> AvatarStartError.Unknown(response.status.value, error)
        }
        return ApiResult.Failure(mapped)
    }

    override suspend fun status(jobId: String): ApiResult<AvatarJobDto, AvatarStatusError> {
        require(jobId.isNotBlank())
        val response = http.get(config.endpoint(ApiVersion.V1, "character-avatars")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(jobId, encodeSlash = true) }
        }
        if (response.status.value == 200) return ApiResult.Success(response.job().also {
            if (it.jobId != jobId) throw InvalidResponseException(response.status.value)
        })
        val error = response.error()
        return ApiResult.Failure(when (ApiErrorCode.fromValue(error.code)) {
            ApiErrorCode.AVATAR_JOB_NOT_FOUND -> AvatarStatusError.NotFound
            ApiErrorCode.UNAVAILABLE -> AvatarStatusError.Unavailable
            else -> AvatarStatusError.Unknown(response.status.value, error)
        })
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T = try {
        json.decodeFromString<T>(bodyAsText())
    } catch (_: SerializationException) { throw InvalidResponseException(status.value) }

    private suspend fun HttpResponse.job(): AvatarJobDto {
        val job = decode<AvatarJobDto>()
        val state = AvatarJobState.entries.firstOrNull { it.name == job.status }
        val valid = job.jobId.matches(Regex("[A-Za-z0-9_-]+")) && when (state) {
            AvatarJobState.QUEUED, AvatarJobState.RUNNING -> job.asset == null && job.error == null
            AvatarJobState.READY -> job.asset?.let { it.id.isNotBlank() && it.url.isNotBlank() } == true && job.error == null
            AvatarJobState.FAILED -> job.asset == null && !job.error.isNullOrBlank()
            null -> false
        }
        if (!valid) throw InvalidResponseException(status.value)
        return job
    }

    private suspend fun HttpResponse.error(): ApiErrorResponse {
        if (status.value !in 400..599) throw InvalidResponseException(status.value)
        return decode<ApiErrorResponse>().also {
            if (ApiErrorCode.fromValue(it.code) in setOf(ApiErrorCode.INVALID_REQUEST, ApiErrorCode.INVALID_INSTALLATION_ID)) {
                throw InvalidRequestException(status.value, it)
            }
        }
    }
}
