package com.dozy.mobile.data

import com.dozy.mobile.domain.Character
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/** Proposed wire shape. The current backend has no character routes; see CHARACTER_DATA.md. */
@Serializable
data class CharacterDto(
    val id: String,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: String,
)

fun Character.toDto() =
    CharacterDto(
        id.value,
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
    )

fun interface CharacterApi {
    /** True only when the server acknowledges persistence. False means local-only. */
    suspend fun save(character: CharacterDto): Boolean
}

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
