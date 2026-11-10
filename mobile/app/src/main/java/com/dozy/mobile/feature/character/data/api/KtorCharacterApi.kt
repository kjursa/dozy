package com.dozy.mobile.feature.character.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Engine and serialization are configured by the platform, never in this common-ready API. */
class KtorCharacterApi(
    private val client: HttpClient,
    private val baseUrl: String?,
) : CharacterApi {
    override suspend fun save(character: CharacterDto): Boolean {
        val url = baseUrl ?: return false
        val response =
            client.put("${url.trimEnd('/')}/characters/${character.id}") {
                contentType(ContentType.Application.Json)
                setBody(character)
            }
        return response.status.value in 200..299
    }
}
