package com.dozy.api.client.characteravatar.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.client.InvalidResponseException
import com.dozy.api.client.businessFailure
import com.dozy.api.client.communicate
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobState
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.api.contracts.v1.characteravatar.AvatarStartError
import com.dozy.api.contracts.v1.characteravatar.AvatarStatusError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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

    override suspend fun start(request: AvatarRequestDto): AvatarStartResult = communicate(AvatarStartResult::TechnicalError) {
        val response = http.post(config.endpoint(ApiVersion.V1, "character-avatars")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        if (response.status.value in 200..299) return@communicate AvatarStartResult.Success(response.job())
        response.businessFailure(AvatarStartError.serializer(), AvatarStartResult::ApiError, AvatarStartResult::TechnicalError)
    }

    override suspend fun status(jobId: String): AvatarStatusResult = communicate(AvatarStatusResult::TechnicalError) {
        require(jobId.isNotBlank())
        val response = http.get(config.endpoint(ApiVersion.V1, "character-avatars")) {
            expectSuccess = false
            header("X-Installation-Id", installations.get())
            url { appendPathSegments(jobId, encodeSlash = true) }
        }
        if (response.status.value in 200..299) return@communicate AvatarStatusResult.Success(response.job().also {
            if (it.jobId != jobId) throw InvalidResponseException(response.status.value)
        })
        response.businessFailure(AvatarStatusError.serializer(), AvatarStatusResult::ApiError, AvatarStatusResult::TechnicalError)
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

}
