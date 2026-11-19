package com.dozy.api.client

import com.dozy.api.client.character.v1.CharactersCreateResult
import com.dozy.api.client.character.v1.CharactersListResult
import com.dozy.api.client.character.v1.CharactersSaveResult
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class CharactersClientTest {
    private val input = SaveCharacterRequest("Oliver", "Curious", "Green eyes", "CHILD")
    private fun sdk(http: HttpClient) =
        DozyClient(http, DozyClientConfig("https://example.test/")).characters.v1

    @Test fun bareArrayAndFullReplacementWireContract() = runTest {
        val http = HttpClient(
            MockEngine { request ->
                if (request.method.value == "GET") {
                    assertEquals("/api/v1/characters", request.url.encodedPath)
                    respond(
                        """[{"id":"one","name":"Oliver","personality":"","appearance":"","type":"CHILD","extra":1}]"""
                    )
                } else {
                    assertEquals("PUT", request.method.value)
                    assertEquals("/api/v1/characters/one%2Ftwo%20three", request.url.encodedPath)
                    val body = (request.body as TextContent).text
                    assertEquals(
                        setOf("name", "personality", "appearance", "type", "avatar"),
                        Json.parseToJsonElement(body).jsonObject.keys
                    )
                    assertEquals(input, Json.decodeFromString<SaveCharacterRequest>(body))
                    respond("", HttpStatusCode.NoContent)
                }
            }
        )
        try {
            assertEquals(
                "Oliver",
                (sdk(http).list() as CharactersListResult.Success).response.single().name
            )
            repeat(2) {
                assertEquals(CharactersSaveResult.Success, sdk(http).save("one/two three", input))
            }
        } finally {
            http.close()
        }
    }

    @Test fun createPostsWithoutAnIdAndReturnsTheServerId() = runTest {
        val key = "019a0000-0000-4000-8000-000000000001"
        val http = HttpClient(
            MockEngine { request ->
                assertEquals("POST", request.method.value)
                assertEquals(key, request.headers["Idempotency-Key"])
                assertEquals("/api/v1/characters", request.url.encodedPath)
                val body = (request.body as TextContent).text
                assertEquals(
                    setOf("name", "personality", "appearance", "type", "avatar"),
                    Json.parseToJsonElement(body).jsonObject.keys
                )
                assertEquals(input, Json.decodeFromString<SaveCharacterRequest>(body))
                respond("""{"id":"server-character"}""", HttpStatusCode.Created)
            }
        )
        try {
            val result =
                assertIs<CharactersCreateResult.Success>(sdk(http).create(input, requestKey = key))
            assertEquals("server-character", result.response.id)
        } finally {
            http.close()
        }
    }
}
