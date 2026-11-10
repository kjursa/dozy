package com.dozy.mobile

import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.character.data.api.KtorCharacterApi
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.domain.CharacterSync
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
                assertTrue(KtorCharacterApi(client, "https://example.test/").save(dto))
                assertFalse(KtorCharacterApi(client, null).save(dto))
            } finally {
                client.close()
            }
        }

    @Test fun ktorDoesNotTreatMissingEndpointAsSynced() =
        runTest {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }) { install(ContentNegotiation) { json() } }
            try {
                assertFalse(KtorCharacterApi(client, "https://example.test").save(character().toDto()))
            } finally {
                client.close()
            }
        }

    private class FakeDao : CharacterDao {
        val rows = MutableStateFlow(emptyList<CharacterEntity>())
        var failAt = 0
        var calls = 0

        override fun observe() = rows

        override suspend fun upsert(character: CharacterEntity) {
            calls++
            if (calls == failAt) error("disk full")
            rows.value = rows.value.filterNot { it.id == character.id } + character
        }
    }
}
