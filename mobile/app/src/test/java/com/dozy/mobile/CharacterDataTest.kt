package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.character.data.api.KtorCharacterApi
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CharacterDataTest {
    @Test fun mapsAllFieldsWithoutSwappingDescriptions() {
        val character = character()
        val entity = character.toEntity(true)
        assertEquals(CharacterEntity("1", "Oliver", "Curious and brave", "Curly brown hair", "CHILD", true), entity)
        assertEquals(character, entity.toDomain())
        assertEquals(CharacterDto("1", "Oliver", "Curious and brave", "Curly brown hair", "CHILD"), character.toDto())
    }

    @Test fun existingRecordsKeepValuesOutsideCurrentCreationRules() {
        val legacy = CharacterEntity("legacy", "x".repeat(55), "", "legacy description", "CHILD", false)
        val domain = legacy.toDomain()
        assertEquals(legacy.name, domain.details.name.value)
        assertEquals(legacy, domain.toEntity(false))
        assertEquals(legacy.name, domain.toDto().name)
    }

    @Test fun persistsBeforeApiThenMarksAcknowledgedRowSynced() =
        runTest {
            val dao = FakeDao()
            val repository =
                DefaultCharacterRepository(
                    dao,
                    CharacterApi {
                        assertEquals(
                            true,
                            dao.rows.value
                                .single()
                                .pendingSync,
                        )
                        true
                    },
                )
            assertEquals(CharacterSync.SYNCED, repository.save(character()).sync)
            assertFalse(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
            assertEquals(listOf(character()), repository.observe().first())
            assertEquals(listOf(character()), DefaultCharacterRepository(dao, CharacterApi { false }).observe().first())
        }

    @Test fun offlineRejectionAndServerFailureRetainDurablePendingRows() =
        runTest {
            listOf(CharacterApi { false }, CharacterApi { throw IllegalStateException("offline") }).forEach { api ->
                val dao = FakeDao()
                assertEquals(CharacterSync.PENDING, DefaultCharacterRepository(dao, api).save(character()).sync)
                assertTrue(
                    dao.rows.value
                        .single()
                        .pendingSync,
                )
            }
        }

    @Test fun localFailureDoesNotCallApi() =
        runTest {
            var called = false
            val dao = FakeDao().apply { failAt = 1 }
            try {
                DefaultCharacterRepository(
                    dao,
                    CharacterApi {
                        called = true
                        true
                    },
                ).save(character())
                fail("Expected disk error")
            } catch (_: IllegalStateException) {
            }
            assertFalse(called)
            assertTrue(dao.rows.value.isEmpty())
        }

    @Test fun syncMetadataFailureStillReportsDurableLocalSave() =
        runTest {
            val dao = FakeDao().apply { failAt = 2 }
            assertEquals(CharacterSync.PENDING, DefaultCharacterRepository(dao, CharacterApi { true }).save(character()).sync)
            assertTrue(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
        }

    @Test fun cancellationKeepsPendingRowAndPropagates() =
        runTest {
            val dao = FakeDao()
            try {
                DefaultCharacterRepository(dao, CharacterApi { throw CancellationException() }).save(character())
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
            assertTrue(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
        }

    @Test fun ktorUsesProposedIdempotentOperationAndSerializedPayload() =
        runTest {
            val dto = character().toDto()
            val engine =
                MockEngine { request ->
                    assertEquals("PUT", request.method.value)
                    assertEquals("https://example.test/characters/1", request.url.toString())
                    val body = (request.body as io.ktor.http.content.TextContent).text
                    assertEquals(dto, Json.decodeFromString<CharacterDto>(body))
                    respond("", HttpStatusCode.NoContent)
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            try {
                assertTrue(KtorCharacterApi(client, ApiConfig("https://example.test/")).save(dto))
            } finally {
                client.close()
            }
        }

    @Test fun ktorDoesNotTreatMissingEndpointAsSynced() =
        runTest {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }) { install(ContentNegotiation) { json() } }
            try {
                assertFalse(KtorCharacterApi(client, ApiConfig("https://example.test")).save(character().toDto()))
            } finally {
                client.close()
            }
        }

    @Test fun disabledApiDoesNotSendRequests() =
        runTest {
            var requests = 0
            val client =
                HttpClient(
                    MockEngine {
                        requests++
                        respond("", HttpStatusCode.NoContent)
                    },
                )
            try {
                assertFalse(KtorCharacterApi(client, ApiConfig(baseUrl = null)).save(character().toDto()))
                assertEquals(0, requests)
            } finally {
                client.close()
            }
        }

    @Test fun refreshMergesPendingEditsAndRemovesOnlySyncedMissingRows() =
        runTest {
            val dao = FakeDao()
            dao.upsert(character().toEntity(true))
            dao.upsert(character("deleted").toEntity(false))
            val repository =
                DefaultCharacterRepository(
                    dao,
                    object : CharacterApi {
                        override suspend fun save(character: CharacterDto) = false

                        override suspend fun list() =
                            listOf(character("1", "Stale server value").toDto(), character("2", "Luna").copy(avatar = avatar()).toDto())
                    },
                )
            assertTrue(repository.refresh())
            assertEquals(listOf("Oliver", "Luna"), repository.observe().first().map { it.details.name.value })
            assertTrue(
                dao.rows.value
                    .first()
                    .pendingSync,
            )
            assertEquals(
                avatar(),
                repository
                    .observe()
                    .first()
                    .last()
                    .avatar,
            )
        }

    @Test fun refreshFailureMalformedSnapshotAndCancellationKeepRows() =
        runTest {
            for (failure in listOf(IllegalStateException("offline"), CancellationException("cancelled"))) {
                val dao = FakeDao()
                dao.upsert(character().toEntity(false))
                val repository =
                    DefaultCharacterRepository(
                        dao,
                        object : CharacterApi {
                            override suspend fun save(character: CharacterDto) = false

                            override suspend fun list(): List<CharacterDto> = throw failure
                        },
                    )
                try {
                    repository.refresh()
                    fail("Expected failure")
                } catch (actual: Exception) {
                    assertSame(failure, actual)
                }
                assertEquals(listOf(character()), repository.observe().first())
            }
            val dao = FakeDao()
            dao.upsert(character().toEntity(false))
            val repository =
                DefaultCharacterRepository(
                    dao,
                    object : CharacterApi {
                        override suspend fun save(character: CharacterDto) = false

                        override suspend fun list() = listOf(character().toDto().copy(type = "UNKNOWN"))
                    },
                )
            try {
                repository.refresh()
                fail("Expected malformed data failure")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals(listOf(character()), repository.observe().first())
            assertFalse(DefaultCharacterRepository(dao, CharacterApi { false }).refresh())
            assertEquals(listOf(character()), repository.observe().first())
        }

    @Test fun listHttpContractHandlesEmptyDisabledAndErrors() =
        runTest {
            var requests = 0
            val engine =
                MockEngine { request ->
                    requests++
                    assertEquals("GET", request.method.value)
                    assertEquals("https://example.test/characters", request.url.toString())
                    when (requests) {
                        1 ->
                            respond(
                                Json.encodeToString(listOf(character().copy(avatar = avatar()).toDto())),
                                HttpStatusCode.OK,
                                io.ktor.http.headersOf("Content-Type", "application/json"),
                            )
                        2 -> respond("[]", HttpStatusCode.OK, io.ktor.http.headersOf("Content-Type", "application/json"))
                        else -> respond("[]", HttpStatusCode.ServiceUnavailable)
                    }
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            try {
                assertNull(KtorCharacterApi(client, ApiConfig(null)).list())
                assertEquals(0, requests)
                val api = KtorCharacterApi(client, ApiConfig("https://example.test/"))
                assertEquals(listOf(character().copy(avatar = avatar()).toDto()), api.list())
                assertEquals(emptyList<CharacterDto>(), api.list())
                try {
                    api.list()
                    fail("Expected HTTP failure")
                } catch (_: IllegalStateException) {
                }
            } finally {
                client.close()
            }
        }

    private class FakeDao : CharacterDao {
        val rows = MutableStateFlow(emptyList<CharacterEntity>())
        var failAt = 0
        var calls = 0

        override suspend fun pendingIds() = rows.value.filter { it.pendingSync }.map { it.id }

        override suspend fun deleteSynced() {
            rows.value = rows.value.filter { it.pendingSync }
        }

        override fun observe() = rows

        override suspend fun upsert(character: CharacterEntity) {
            calls++
            if (calls == failAt) error("disk full")
            rows.value = rows.value.filterNot { it.id == character.id } + character
        }
    }
}
