package com.dozy.mobile.feature.character.data.api

import com.dozy.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.delay

/** Engine and serialization are configured by the platform, never in this common-ready API. */
class KtorCharacterApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : CharacterApi {
    override suspend fun delete(id: String): Boolean {
        val url = config.apiBaseUrl ?: return false
        val response = client.delete("${url.trimEnd('/')}/characters") { url { appendPathSegments(id, encodeSlash = true) } }
        check(response.status.value == 204 || response.status.value == 404) { "Character deletion failed" }
        return true
    }

    override suspend fun list(): List<CharacterDto>? {
        val url = config.apiBaseUrl ?: return null
        val response = client.get("${url.trimEnd('/')}/characters")
        check(response.status.value == 200) { "Character collection unavailable" }
        return response.body()
    }

    override suspend fun generateAvatar(input: AvatarRequestDto): AvatarDto? {
        val url = config.apiBaseUrl ?: return null
        val response =
            client.post("${url.trimEnd('/')}/character-avatars") {
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
            val status = client.get("${url.trimEnd('/')}/character-avatars/$jobId")
            check(status.status.value == 200) { "Avatar status unavailable" }
            job = status.body<AvatarJobDto>()
        }
        error("Avatar generation timed out")
    }

    override suspend fun save(character: CharacterDto): Boolean {
        val url = config.apiBaseUrl ?: return false
        val response =
            client.put("${url.trimEnd('/')}/characters") {
                url { appendPathSegments(character.id, encodeSlash = true) }
                contentType(ContentType.Application.Json)
                setBody(character)
            }
        return response.status.value in 200..299
    }
}
