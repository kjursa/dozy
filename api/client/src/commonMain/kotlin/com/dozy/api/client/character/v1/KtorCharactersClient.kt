package com.dozy.api.client.character.v1

import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.businessFailure
import com.dozy.api.client.communicate
import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharactersDeleteError
import com.dozy.api.contracts.v1.character.CharactersListError
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.api.contracts.v1.character.CreatedCharacterResponse
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

internal class KtorCharactersClient(
    private val http: HttpClient,
    private val config: DozyClientConfig
) : CharactersClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun list(): CharactersListResult =
        communicate(CharactersListResult::TechnicalError) {
            val response = http.get(config.endpoint(ApiVersion.V1, "characters")) {
                expectSuccess = false
            }
            if (response.status.value in 200..299) {
                return@communicate CharactersListResult.Success(
                    json.decodeFromString<List<CharacterDto>>(response.bodyAsText())
                )
            }
            response.businessFailure(
                CharactersListError.serializer(),
                CharactersListResult::ApiError,
                CharactersListResult::TechnicalError
            )
        }

    override suspend fun create(
        request: SaveCharacterRequest,
        requestKey: String?
    ): CharactersCreateResult = communicate(CharactersCreateResult::TechnicalError) {
        val response = http.post(config.endpoint(ApiVersion.V1, "characters")) {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            requestKey?.let { header("Idempotency-Key", it) }
            setBody(json.encodeToString(request))
        }
        if (response.status == HttpStatusCode.Created) {
            val created = json.decodeFromString<CreatedCharacterResponse>(response.bodyAsText())
            require(created.id.isNotBlank())
            CharactersCreateResult.Success(created)
        } else {
            response.businessFailure(
                CharactersSaveError.serializer(),
                CharactersCreateResult::ApiError,
                CharactersCreateResult::TechnicalError
            )
        }
    }

    override suspend fun save(id: String, request: SaveCharacterRequest): CharactersSaveResult =
        communicate(CharactersSaveResult::TechnicalError) {
            require(id.isNotBlank())
            val response = http.put(config.endpoint(ApiVersion.V1, "characters")) {
                expectSuccess = false
                url { appendPathSegments(id, encodeSlash = true) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (response.status.value in 200..299) return@communicate CharactersSaveResult.Success
            response.businessFailure(
                CharactersSaveError.serializer(),
                CharactersSaveResult::ApiError,
                CharactersSaveResult::TechnicalError
            )
        }

    override suspend fun delete(id: String): CharactersDeleteResult =
        communicate(CharactersDeleteResult::TechnicalError) {
            require(id.isNotBlank())
            val response = http.delete(config.endpoint(ApiVersion.V1, "characters")) {
                expectSuccess = false
                url { appendPathSegments(id, encodeSlash = true) }
            }
            if (response.status.value in 200..299) return@communicate CharactersDeleteResult.Success
            response.businessFailure(CharactersDeleteError.serializer(), {
                if (it ==
                    CharactersDeleteError.NotFound
                ) {
                    CharactersDeleteResult.Success
                } else {
                    CharactersDeleteResult.ApiError(it)
                }
            }, CharactersDeleteResult::TechnicalError)
        }
}
