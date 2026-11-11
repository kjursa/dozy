package com.dozy.mobile.feature.story.data.api

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.place.data.api.PlaceDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
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

interface GenerationApi {
    suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto?
}

class KtorGenerationApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : GenerationApi {
    override suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto? {
        val base = config.baseUrl ?: return null
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
