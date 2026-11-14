package com.dozy.mobile

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.DozyApiException
import com.dozy.api.client.place.PlacesClient
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.SavePlaceRequestDto
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.place.data.toSaveRequest
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.test.assertFailsWith

class PlaceSaveTest {
    @Test fun savesBeforeHttpAndRetriesDurablePendingRowsWithSameId() =
        runTest {
            val dao = FakePlaceDao()
            var available = false
            val ids = mutableListOf<String>()
            val api =
                object : PlacesClient {
                    override suspend fun list() = emptyList<com.dozy.mobile.feature.place.data.api.PlaceDto>()

                    override suspend fun save(
                        id: String,
                        request: SavePlaceRequestDto,
                    ) {
                        assertEquals(request.name, dao.find(id)?.name)
                        ids += id
                        check(available)
                    }
                }
            val repository = DefaultPlaceRepository(dao, api, emptyList())
            assertEquals(PlaceSync.PENDING, repository.save(place()))
            assertTrue(dao.find("custom")!!.pendingSync)
            available = true
            // A fresh repository must discover pending work from storage.
            DefaultPlaceRepository(dao, api, emptyList()).refresh()
            assertEquals(listOf("custom", "custom"), ids)
            assertFalse(dao.find("custom")!!.pendingSync)
            repository.refresh()
            assertEquals(2, ids.size)
            assertEquals(1, dao.rows.value.size)
        }

    @Test fun storageFailureDoesNotRequestHttpAndCancellationLeavesDurablePendingRow() =
        runTest {
            val dao = FakePlaceDao()
            var calls = 0
            val api =
                object : PlacesClient {
                    override suspend fun list(): List<com.dozy.mobile.feature.place.data.api.PlaceDto> =
                        throw com.dozy.api.client
                            .ApiDisabledException()

                    override suspend fun save(
                        id: String,
                        request: SavePlaceRequestDto,
                    ) {
                        calls++
                        throw CancellationException()
                    }
                }
            val repository = DefaultPlaceRepository(dao, api, emptyList())
            dao.failure = true
            try {
                repository.save(place())
                fail("Expected storage failure")
            } catch (_: IllegalStateException) {
            }
            assertEquals(0, calls)
            dao.failure = false
            try {
                repository.save(place())
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
            assertTrue(dao.find("custom")!!.pendingSync)
        }

    @Test fun putUsesStableEncodedIdAndRoundTripsFieldsAndDisabledApiMakesNoRequests() =
        runTest {
            var calls = 0
            val requestDto = place("custom/id").toSaveRequest().copy(inhabitants = "Bees")
            val client =
                HttpClient(
                    MockEngine { request ->
                        calls++
                        assertEquals("PUT", request.method.value)
                        assertEquals("/api/v1/places/custom%2Fid", request.url.encodedPath)
                        val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                        assertEquals(requestDto, Json.decodeFromString<SavePlaceRequestDto>(body))
                        assertTrue(
                            Json
                                .parseToJsonElement(body)
                                .jsonObject.keys
                                .all { it in setOf("name", "description", "inhabitants") },
                        )
                        respond("", if (calls == 1) HttpStatusCode.NoContent else HttpStatusCode.ServiceUnavailable)
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                assertFailsWith<ApiDisabledException> { placesClient(client, ApiConfig(null)).save("custom/id", requestDto) }
                assertEquals(0, calls)
                val api = placesClient(client, ApiConfig("https://example.test/"))
                api.save("custom/id", requestDto)
                assertFailsWith<DozyApiException> { api.save("custom/id", requestDto) }
            } finally {
                client.close()
            }
        }

    @Test fun editingReplacesOneRowAndRetriesLatestFieldsIncludingClearedInhabitants() =
        runTest {
            val dao = FakePlaceDao()
            val original = place().copy(inhabitants = Place.Inhabitants("Bees"))
            dao.upsert(original.toEntity().copy(pendingSync = false))
            val requests = mutableListOf<SavePlaceRequestDto>()
            val repository =
                DefaultPlaceRepository(
                    dao,
                    object : PlacesClient {
                        override suspend fun list() = emptyList<com.dozy.mobile.feature.place.data.api.PlaceDto>()

                        override suspend fun save(
                            id: String,
                            request: SavePlaceRequestDto,
                        ) {
                            requests += request
                            error("Unavailable")
                        }
                    },
                    emptyList(),
                )
            val updated = original.copy(name = Place.Name("New garden"), inhabitants = null)
            assertEquals(PlaceSync.PENDING, repository.save(updated))
            assertEquals(listOf(updated), repository.observe().first())
            repository.refresh()
            assertEquals(listOf(updated.toSaveRequest(), updated.toSaveRequest()), requests)
            assertEquals(1, dao.rows.value.size)
            assertTrue(dao.find("custom")!!.pendingSync)
        }

    @Test fun repositoryRejectsForgedPermissionForStoredAndBundledPlaces() =
        runTest {
            val dao = FakePlaceDao()
            val locked = place("locked", editable = false)
            val predefined = place("remote", custom = false)
            dao.upsert(locked.toEntity())
            dao.upsert(predefined.toEntity().copy(local = false))
            val before = dao.rows.value
            val repository =
                DefaultPlaceRepository(dao, PlacesClient { error("No HTTP allowed") }, listOf(place("builtin:woods", custom = false)))
            for (id in listOf("locked", "remote", "builtin:woods", "builtin:unknown")) {
                try {
                    repository.save(place(id))
                    fail("Expected permission rejection for $id")
                } catch (_: IllegalArgumentException) {
                }
            }
            assertEquals(before, dao.rows.value)
        }

    @Test fun rejectedEditsNeverAcknowledgeRemoteSuccessAndOmittedInhabitantsClearTheField() =
        runTest {
            val statuses = listOf(403, 404, 409, 422, 429, 500)
            var calls = 0
            val client =
                HttpClient(
                    MockEngine { request ->
                        val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                        assertEquals(null, Json.decodeFromString<SavePlaceRequestDto>(body).inhabitants)
                        respond("", HttpStatusCode.fromValue(statuses[calls++]))
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                val api = placesClient(client, ApiConfig("https://example.test"))
                statuses.forEach { assertFailsWith<DozyApiException> { api.save("custom", place().toSaveRequest()) } }
                assertEquals(statuses.size, calls)
            } finally {
                client.close()
            }
        }
}
