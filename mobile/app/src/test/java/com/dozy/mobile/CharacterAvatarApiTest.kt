package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.api.*
import com.dozy.mobile.feature.character.data.database.*
import com.dozy.mobile.feature.character.data.toAvatarDto
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CharacterAvatarApiTest {
    @Test fun submitsDescriptionsThenPollsSameJobUntilReady() =
        runTest {
            var requests = 0
            val client =
                client { request ->
                    requests++
                    when (requests) {
                        1 -> {
                            assertEquals(HttpMethod.Post, request.method)
                            assertEquals("/character-avatars", request.url.encodedPath)
                            assertEquals(
                                createInput().toAvatarDto(),
                                Json.decodeFromString<AvatarRequestDto>((request.body as TextContent).text),
                            )
                            respond("""{"jobId":"job-1","status":"QUEUED"}""", HttpStatusCode.Accepted, headers)
                        }
                        2 -> {
                            assertEquals(HttpMethod.Get, request.method)
                            assertEquals("/character-avatars/job-1", request.url.encodedPath)
                            respond("""{"jobId":"job-1","status":"RUNNING"}""", HttpStatusCode.OK, headers)
                        }
                        else ->
                            respond(
                                """{"jobId":"job-1","status":"READY","asset":{"id":"asset","url":"https://example.test/avatar.png"}}""",
                                HttpStatusCode.OK,
                                headers,
                            )
                    }
                }
            try {
                assertEquals(
                    AvatarDto("asset", "https://example.test/avatar.png"),
                    KtorCharacterApi(client, ApiConfig("https://example.test/")).generateAvatar(createInput().toAvatarDto()),
                )
                assertEquals(3, requests)
            } finally {
                client.close()
            }
        }

    @Test fun unavailableMakesNoRequestsAndNeverWritesCharacter() =
        runTest {
            val client = client { error("Disabled API must not send requests") }
            try {
                val result = GenerateCharacterAvatar(repository(KtorCharacterApi(client, ApiConfig(null))))(createInput())
                assertEquals(GenerateCharacterAvatar.Result.Unavailable, result)
            } finally {
                client.close()
            }
        }

    @Test fun httpFailuresAndMalformedJobsAreRetryableFailuresAndNeverWriteCharacters() =
        runTest {
            val responses =
                listOf(
                    HttpStatusCode.InternalServerError to "{}",
                    HttpStatusCode.Accepted to """{"jobId":"job","status":"FAILED","error":"GENERATION_FAILED"}""",
                    HttpStatusCode.Accepted to """{"jobId":"job","status":"READY"}""",
                    HttpStatusCode.Accepted to """{"jobId":"job","status":"READY","asset":{"id":"","url":"http://invalid"}}""",
                    HttpStatusCode.Accepted to """{"jobId":"../bad","status":"QUEUED"}""",
                    HttpStatusCode.Accepted to """{"jobId":"job","status":"UNKNOWN"}""",
                )
            for ((status, body) in responses) {
                val client = client { respond(body, status, headers) }
                try {
                    val generate = GenerateCharacterAvatar(repository(KtorCharacterApi(client, ApiConfig("https://example.test"))))
                    assertEquals(GenerateCharacterAvatar.Result.Failure, generate(createInput()))
                    assertEquals(GenerateCharacterAvatar.Result.Failure, generate(createInput()))
                } finally {
                    client.close()
                }
            }
        }

    @Test fun pollingHttpFailureIsNotReadyAndCancellationPropagates() =
        runTest {
            var requests = 0
            val client =
                client {
                    requests++
                    if (requests == 1) {
                        respond("""{"jobId":"job","status":"QUEUED"}""", HttpStatusCode.Accepted, headers)
                    } else {
                        respond("", HttpStatusCode.ServiceUnavailable)
                    }
                }
            try {
                assertEquals(
                    GenerateCharacterAvatar.Result.Failure,
                    GenerateCharacterAvatar(repository(KtorCharacterApi(client, ApiConfig("https://example.test"))))(createInput()),
                )
            } finally {
                client.close()
            }
            val cancelled =
                object : CharacterApi {
                    override suspend fun save(character: CharacterDto) = false

                    override suspend fun generateAvatar(input: AvatarRequestDto): AvatarDto? = throw CancellationException()
                }
            try {
                GenerateCharacterAvatar(repository(cancelled))(createInput())
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }

    @Test fun avatarRoundTripsThroughStorageAndSaveDto() {
        val character = character().copy(avatar = avatar())
        assertEquals(character, character.toEntity(true).toDomain())
        assertEquals(AvatarDto("asset", "https://example.test/avatar.png"), character.toDto().avatar)
    }

    private fun repository(api: CharacterApi) =
        DefaultCharacterRepository(
            object : CharacterDao {
                override suspend fun pendingIds() = emptyList<String>()

                override suspend fun deleteSynced() = Unit

                override fun observe() = flowOf(emptyList<CharacterEntity>())

                override suspend fun upsert(character: CharacterEntity) {
                    error("Generation must never write a character")
                }
            },
            api,
        )

    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) { install(ContentNegotiation) { json() } }

    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
}
