package com.dozy.api.client

import com.dozy.api.client.character.v1.CharactersDeleteResult
import com.dozy.api.client.character.v1.CharactersListResult
import com.dozy.api.client.character.v1.CharactersSaveResult
import com.dozy.api.client.characteravatar.v1.AvatarStartResult
import com.dozy.api.client.characteravatar.v1.AvatarStatusResult
import com.dozy.api.client.story.v1.StoryListResult
import com.dozy.api.client.story.v1.StoryReadResult
import com.dozy.api.client.storygeneration.v1.GenerationRetryResult
import com.dozy.api.client.storygeneration.v1.GenerationStartResult
import com.dozy.api.client.storygeneration.v1.GenerationStatusResult
import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.character.CharactersDeleteError
import com.dozy.api.contracts.v1.character.CharactersListError
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.api.contracts.v1.characteravatar.AvatarStartError
import com.dozy.api.contracts.v1.characteravatar.AvatarStatusError
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusError
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryReadError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class ClientResultsTest {
    private data class Operation(
        val name: String,
        val invoke: suspend (DozyClient) -> Any,
        val technical: (Any) -> TechnicalError,
        val api: (Any) -> ApiBusinessError,
        val known: List<ApiBusinessError>,
    )
    private val operations = listOf(
        Operation("CharactersList", { it.characters.v1.list() }, { (it as CharactersListResult.TechnicalError).error }, { (it as CharactersListResult.ApiError).error }, CharactersListError.known),
        Operation("CharactersSave", { it.characters.v1.save("id", SaveCharacterRequest("Name", "Kind", "Eyes", "CHILD")) }, { (it as CharactersSaveResult.TechnicalError).error }, { (it as CharactersSaveResult.ApiError).error }, CharactersSaveError.known),
        Operation("CharactersDelete", { it.characters.v1.delete("id") }, { (it as CharactersDeleteResult.TechnicalError).error }, { (it as CharactersDeleteResult.ApiError).error }, CharactersDeleteError.known),
        Operation("AvatarStart", { it.characterAvatars.v1.start(AvatarRequestDto("CHILD", "Kind", "Eyes")) }, { (it as AvatarStartResult.TechnicalError).error }, { (it as AvatarStartResult.ApiError).error }, AvatarStartError.known),
        Operation("AvatarStatus", { it.characterAvatars.v1.status("job") }, { (it as AvatarStatusResult.TechnicalError).error }, { (it as AvatarStatusResult.ApiError).error }, AvatarStatusError.known),
        Operation("GenerationStart", { it.storyGenerations.v1.start("key", GenerationRequestDto(emptyList(), "AUTOMATIC", "SHORT")) }, { (it as GenerationStartResult.TechnicalError).error }, { (it as GenerationStartResult.ApiError).error }, GenerationStartError.known),
        Operation("GenerationStatus", { it.storyGenerations.v1.status("job") }, { (it as GenerationStatusResult.TechnicalError).error }, { (it as GenerationStatusResult.ApiError).error }, GenerationStatusError.known),
        Operation("GenerationRetry", { it.storyGenerations.v1.retry("job", "key") }, { (it as GenerationRetryResult.TechnicalError).error }, { (it as GenerationRetryResult.ApiError).error }, GenerationRetryError.known),
        Operation("StoryList", { it.stories.v1.list(ListStoriesRequest()) }, { (it as StoryListResult.TechnicalError).error }, { (it as StoryListResult.ApiError).error }, StoryListError.known),
        Operation("StoryRead", { it.stories.v1.read("story") }, { (it as StoryReadResult.TechnicalError).error }, { (it as StoryReadResult.ApiError).error }, StoryReadError.known),
    )
    private fun sdk(http: HttpClient, baseUrl: String? = "https://example.test") = DozyClient(http, DozyClientConfig(baseUrl))

    @Test fun `every operation classifies 5xx before reading HTML empty or business bodies`() = runTest {
        for (operation in operations) for (status in listOf(500, 503, 504)) for (body in listOf("", "<html>Error</html>", """{"code":"JOB_NOT_FOUND"}""")) {
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(status)) }) { expectSuccess = true }
            try { assertEquals(TechnicalError.Server(status), operation.technical(operation.invoke(sdk(http))), operation.name) }
            finally { http.close() }
        }
    }

    @Test fun `every operation returns technical failures for transport local errors malformed envelopes and disabled API`() = runTest {
        for (operation in operations) {
            for (failure in listOf(IOException("Offline"), IllegalArgumentException("Bad argument"), IllegalStateException("Bug"))) {
                val http = HttpClient(MockEngine { throw failure })
                try { assertEquals(failure.message, assertIs<TechnicalError.Communication>(operation.technical(operation.invoke(sdk(http)))).exception.message, operation.name) }
                finally { http.close() }
            }
            for (body in listOf("{}", "<html>Invalid</html>", """{"code":"INVALID_REQUEST"}""", """{"code":"INVALID_INSTALLATION_ID"}""")) {
                val http = HttpClient(MockEngine { respond(body, HttpStatusCode.BadRequest) })
                try { assertIs<TechnicalError.Communication>(operation.technical(operation.invoke(sdk(http))), operation.name) }
                finally { http.close() }
            }
            val http = HttpClient(MockEngine { error("No request allowed") })
            try { assertIs<ApiDisabledException>(assertIs<TechnicalError.Communication>(operation.technical(operation.invoke(sdk(http, null)))).exception) }
            finally { http.close() }
        }
    }

    @Test fun `every operation decodes unknown codes through its shared contract`() = runTest {
        val http = HttpClient(MockEngine { respond("""{"code":"validation.future.new_rule"}""", HttpStatusCode.UnprocessableEntity) })
        try { operations.forEach { assertEquals("validation.future.new_rule", it.api(it.invoke(sdk(http))).code, it.name) } }
        finally { http.close() }
    }

    @Test fun `known contract errors are returned directly with no client model translation`() = runTest {
        for (operation in operations.filterNot { it.name == "CharactersDelete" }) for (error in operation.known) {
            val http = HttpClient(MockEngine { respond("""{"code":"${error.code}"}""", HttpStatusCode.UnprocessableEntity) })
            try { assertEquals(error, operation.api(operation.invoke(sdk(http))), operation.name) }
            finally { http.close() }
        }
    }

    @Test fun `payload operations reject malformed successful bodies as communication failures`() = runTest {
        val http = HttpClient(MockEngine { respond("{}") })
        try {
            operations.filterNot { it.name in setOf("CharactersSave", "CharactersDelete") }.forEach { operation ->
                assertIs<TechnicalError.Communication>(operation.technical(operation.invoke(sdk(http))), operation.name)
            }
        } finally { http.close() }
    }

    @Test fun `every operation propagates cancellation`() = runTest {
        val http = HttpClient(MockEngine { throw CancellationException("Cancelled") })
        try { operations.forEach { operation -> assertFailsWith<CancellationException> { operation.invoke(sdk(http)) } } }
        finally { http.close() }
    }

    @Test fun `payload-free character writes accept any successful status`() = runTest {
        for (status in listOf(200, 201, 202, 204, 299)) {
            val http = HttpClient(MockEngine { respond("", HttpStatusCode.fromValue(status)) })
            try {
                assertEquals(CharactersSaveResult.Success, sdk(http).characters.v1.save("id", SaveCharacterRequest("Name", "Kind", "Eyes", "CHILD")))
                assertEquals(CharactersDeleteResult.Success, sdk(http).characters.v1.delete("id"))
            } finally { http.close() }
        }
    }

    @Test fun `known character not found acknowledges deletion but unknown does not`() = runTest {
        val http = HttpClient(MockEngine { respond("""{"code":"CHARACTER_NOT_FOUND"}""", HttpStatusCode.NotFound) })
        try { assertEquals(CharactersDeleteResult.Success, sdk(http).characters.v1.delete("id")) }
        finally { http.close() }
    }
}
