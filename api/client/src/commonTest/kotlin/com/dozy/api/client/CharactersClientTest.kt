package com.dozy.api.client

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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class CharactersClientTest {
    private val input = SaveCharacterRequest("Oliver", "Curious", "Green eyes", "CHILD")
    private fun sdk(http: HttpClient) = DozyClient(http, DozyClientConfig("https://example.test/"), InstallationIdProvider { "installation" }).characters.v1

    @Test fun bareArrayAndFullReplacementWireContract() = runTest {
        var writes = 0
        val http = HttpClient(MockEngine { request ->
            assertEquals("installation", request.headers["X-Installation-Id"])
            if (request.method.value == "GET") {
                assertEquals("/api/v1/characters", request.url.encodedPath)
                respond("""[{"id":"one","name":"Oliver","personality":"","appearance":"","type":"CHILD","extra":1}]""")
            } else {
                assertEquals("PUT", request.method.value)
                assertEquals("/api/v1/characters/one%2Ftwo%20three", request.url.encodedPath)
                val body = (request.body as TextContent).text
                assertEquals(setOf("name", "personality", "appearance", "type", "avatar"), Json.parseToJsonElement(body).jsonObject.keys)
                assertEquals(input, Json.decodeFromString<SaveCharacterRequest>(body))
                respond("", if (writes++ == 0) HttpStatusCode.Created else HttpStatusCode.NoContent)
            }
        })
        try {
            assertEquals("Oliver", (sdk(http).list() as CharactersListResult.Success).response.single().name)
            repeat(2) { assertEquals(CharactersSaveResult.Success, sdk(http).save("one/two three", input)) }
        } finally { http.close() }
    }

}
