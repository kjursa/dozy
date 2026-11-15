package com.dozy.mobile.feature.character.data.api

import com.dozy.api.client.InstallationIdProvider
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay

/** Engine and serialization are configured by the platform, never in this common-ready API. */
class KtorCharacterApi(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val installations: InstallationIdProvider,
) : CharacterApi {
    override suspend fun generateAvatar(input: AvatarRequestDto): AvatarDto? {
        val url = config.apiBaseUrl ?: return null
        val response =
            client.post("${url.trimEnd('/')}/character-avatars") {
                header("X-Installation-Id", installations.get())
                contentType(ContentType.Application.Json)
                setBody(input)
            }
        check(response.status.value == 202) { "Avatar submission failed" }
        var job = response.body<AvatarJobDto>()
        val jobId = job.jobId
        require(jobId.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid job identifier" }
        // Bound client observation; retry starts a new explicitly requested attempt.
        repeat(121) { poll ->
            check(job.jobId == jobId) { "Mismatched avatar job" }
            when (job.status) {
                "READY" -> return requireNotNull(job.asset)
                "FAILED" -> error("Avatar generation failed")
                "QUEUED", "RUNNING" -> Unit
                else -> error("Unknown avatar status")
            }
            check(poll < 120) { "Avatar generation timed out" }
            delay(1000)
            val status =
                client.get("${url.trimEnd('/')}/character-avatars/$jobId") {
                    header("X-Installation-Id", installations.get())
                }
            check(status.status.value == 200) { "Avatar status unavailable" }
            job = status.body<AvatarJobDto>()
        }
        error("Avatar generation timed out")
    }
}
