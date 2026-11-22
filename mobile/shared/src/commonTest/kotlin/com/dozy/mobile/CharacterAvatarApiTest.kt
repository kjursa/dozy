package com.dozy.mobile

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.characteravatar.CharacterAvatarsClients
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.data.database.SyncStateQueries
import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.data.toAvatarDto
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import fixtures.CharactersClientFixture
import fixtures.FakeSyncStateQueries
import fixtures.avatarClients
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
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
                            assertEquals("/api/v1/character-avatars", request.url.encodedPath)
                            assertEquals(
                                createInput().toAvatarDto(),
                                Json.decodeFromString<AvatarRequestDto>((request.body as TextContent).text),
                            )
                            respond("""{"jobId":"job-1","status":"QUEUED"}""", HttpStatusCode.Accepted, headers)
                        }
                        2 -> {
                            assertEquals(HttpMethod.Get, request.method)
                            assertEquals("/api/v1/character-avatars/job-1", request.url.encodedPath)
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
                    repository(
                        avatarClient(
                            client,
                            ApiConfig("https://example.test/"),
                        ),
                    ).generateAvatar(createInput())?.let { AvatarDto(it.id.value, it.url.value) },
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
                val result =
                    GenerateCharacterAvatar(
                        repository(avatarClient(client, ApiConfig(null))),
                    )(createInput())
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
                    val generate =
                        GenerateCharacterAvatar(
                            repository(
                                avatarClient(client, ApiConfig("https://example.test")),
                            ),
                        )
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
                    GenerateCharacterAvatar(
                        repository(
                            avatarClient(
                                client,
                                ApiConfig("https://example.test"),
                            ),
                        ),
                    )(createInput()),
                )
            } finally {
                client.close()
            }
            val cancelled = avatarClients { throw CancellationException() }
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

    @Test fun pollingStopsAt120GetsAndExitCancelsObservation() =
        runTest {
            var posts = 0
            var gets = 0
            val secondPost = CompletableDeferred<Unit>()
            val http =
                client { request ->
                    if (request.method == HttpMethod.Post) {
                        if (++posts == 2) secondPost.complete(Unit)
                    } else {
                        gets++
                    }
                    respond(
                        """{"jobId":"job","status":"RUNNING"}""",
                        if (request.method ==
                            HttpMethod.Post
                        ) {
                            HttpStatusCode.Accepted
                        } else {
                            HttpStatusCode.OK
                        },
                        headers,
                    )
                }
            try {
                val repository =
                    repository(avatarClient(http, ApiConfig("https://example.test")))
                assertEquals(GenerateCharacterAvatar.Result.Failure, GenerateCharacterAvatar(repository)(createInput()))
                assertEquals(1, posts)
                assertEquals(120, gets)
                val observing = launch { repository.generateAvatar(createInput()) }
                secondPost.await()
                observing.cancel()
                observing.join()
                advanceTimeBy(5000)
                runCurrent()
                assertEquals(2, posts)
                assertEquals(120, gets)
            } finally {
                http.close()
            }
        }

    @Test fun developmentAssetsRequireExplicitLocalMode() {
        val local = AvatarDto("asset", "http://10.0.2.2:8080/assets/asset")
        assertFailsWith<IllegalArgumentException> { local.toDomain() }
        assertEquals(local.url, local.toDomain(true).url.value)
        assertFailsWith<IllegalArgumentException> { AvatarDto("asset", "http://example.com/image").toDomain(true) }
    }

    private fun repository(api: CharacterAvatarsClients) =
        DefaultCharacterRepository(
            object : CharacterDao, SyncStateQueries by FakeSyncStateQueries() {
                override fun observe(id: String) = flowOf<CharacterEntity?>(null)

                override suspend fun deletedIds() = emptyList<String>()

                override suspend fun markDeleted(id: String) = error("Unexpected deletion")

                override suspend fun pendingIds() = emptyList<String>()

                override suspend fun deleteSynced() = Unit

                override fun observe() = flowOf(emptyList<CharacterEntity>())

                override suspend fun updateExisting(
                    id: String,
                    name: String,
                    personality: String,
                    appearance: String,
                    type: String,
                    avatarId: String?,
                    avatarUrl: String?,
                ): Int = error("Generation must never update a character")

                override suspend fun upsert(character: CharacterEntity) {
                    error("Generation must never write a character")
                }
            },
            CharactersClientFixture(),
            api,
        )

    private fun avatarClient(
        client: HttpClient,
        config: ApiConfig,
    ) = DozyClient(client, DozyClientConfig(config.baseUrl)).characterAvatars

    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) { install(ContentNegotiation) { json() } }

    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
}
