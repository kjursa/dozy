package com.dozy.mobile

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.place.v1.PlacesClient
import com.dozy.api.client.place.v1.PlacesCreateResult
import com.dozy.api.client.place.v1.PlacesListResult
import com.dozy.api.client.place.v1.PlacesSaveResult
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.PlacesDto
import com.dozy.mobile.feature.place.data.api.SavePlaceRequestDto
import com.dozy.mobile.feature.place.data.toDto
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

class PlaceSaveTest {
    @Test fun savesBeforeHttpAndRetriesDurablePendingRowsWithSameId() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place().toEntity().copy(pendingSync = false))
            var available = false
            val ids = mutableListOf<String>()
            val api =
                object : PlacesClient {
                    override suspend fun list(ifNoneMatch: String?) = PlacesListResult.Success(PlacesDto(listOf(place().toDto())))

                    override suspend fun create(
                        request: SavePlaceRequestDto,
                        requestKey: String?,
                    ): PlacesCreateResult = error("Unexpected place creation")

                    override suspend fun save(
                        id: String,
                        request: SavePlaceRequestDto,
                    ): PlacesSaveResult {
                        assertEquals(request.name, dao.find(id)?.name)
                        ids += id
                        return if (available) PlacesSaveResult.Success else PlacesSaveResult.TechnicalError(TechnicalError.Server(503))
                    }
                }
            val repository = DefaultPlaceRepository(dao, fakePlacesClients(api), emptyList())
            assertEquals(PlaceSync.PENDING, repository.save(place()))
            assertTrue(dao.find("custom")!!.pendingSync)
            available = true
            // A fresh repository must discover pending work from storage.
            DefaultPlaceRepository(dao, fakePlacesClients(api), emptyList()).refresh()
            assertEquals(listOf("custom", "custom"), ids)
            assertFalse(dao.find("custom")!!.pendingSync)
            repository.refresh()
            assertEquals(2, ids.size)
            assertEquals(1, dao.rows.value.size)
        }

    @Test fun typedApiFailureKeepsTheDurableWritePending() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place().toEntity().copy(pendingSync = false))
            val api =
                object : PlacesClient {
                    override suspend fun list(ifNoneMatch: String?) = PlacesListResult.Success(PlacesDto(emptyList()))

                    override suspend fun create(
                        request: SavePlaceRequestDto,
                        requestKey: String?,
                    ): PlacesCreateResult = error("Unexpected place creation")

                    override suspend fun save(
                        id: String,
                        request: SavePlaceRequestDto,
                    ): PlacesSaveResult = PlacesSaveResult.ApiError(PlacesSaveError.Conflict)
                }
            val repository = DefaultPlaceRepository(dao, fakePlacesClients(api), emptyList())
            assertEquals(PlaceSync.PENDING, repository.save(place()))
            assertTrue(dao.find("custom")!!.pendingSync)
            repository.refresh()
            assertTrue(dao.find("custom")!!.pendingSync)
            assertEquals(listOf(place()), repository.observe().first())
        }

    @Test fun storageFailureDoesNotRequestHttpAndCancellationLeavesDurablePendingRow() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place().toEntity().copy(pendingSync = false))
            var calls = 0
            val api =
                object : PlacesClient {
                    override suspend fun list(ifNoneMatch: String?): PlacesListResult =
                        PlacesListResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))

                    override suspend fun create(
                        request: SavePlaceRequestDto,
                        requestKey: String?,
                    ): PlacesCreateResult = error("Unexpected place creation")

                    override suspend fun save(
                        id: String,
                        request: SavePlaceRequestDto,
                    ): PlacesSaveResult {
                        calls++
                        throw CancellationException()
                    }
                }
            val repository = DefaultPlaceRepository(dao, fakePlacesClients(api), emptyList())
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
                        respond(
                            if (calls ==
                                1
                            ) {
                                ""
                            } else {
                                """{"code":"UNAVAILABLE"}"""
                            },
                            if (calls ==
                                1
                            ) {
                                HttpStatusCode.NoContent
                            } else {
                                HttpStatusCode.ServiceUnavailable
                            },
                        )
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                assertTrue(
                    apiFailure(placesClient(client, ApiConfig(null)).save("custom/id", requestDto)) is TechnicalError.Communication,
                )
                assertEquals(0, calls)
                val api = placesClient(client, ApiConfig("https://example.test/"))
                apiValue(api.save("custom/id", requestDto))
                assertEquals(TechnicalError.Server(503), apiFailure(api.save("custom/id", requestDto)))
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
                    fakePlacesClients(
                        object : PlacesClient {
                            override suspend fun list(ifNoneMatch: String?) = PlacesListResult.Success(PlacesDto(emptyList()))

                            override suspend fun create(
                                request: SavePlaceRequestDto,
                                requestKey: String?,
                            ): PlacesCreateResult = error("Unexpected place creation")

                            override suspend fun save(
                                id: String,
                                request: SavePlaceRequestDto,
                            ): PlacesSaveResult {
                                requests += request
                                return PlacesSaveResult.TechnicalError(TechnicalError.Server(503))
                            }
                        },
                    ),
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
                DefaultPlaceRepository(
                    dao,
                    fakePlacesClients(fakePlacesClient { error("No HTTP allowed") }),
                    listOf(place("place-00000000-0000-7000-8000-000000000001", custom = false)),
                )
            for (id in listOf(
                "locked",
                "remote",
                "place-00000000-0000-7000-8000-000000000001",
                "place-00000000-0000-7000-8000-000000000004",
            )) {
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
                        val status = statuses[calls++]
                        val error =
                            when (status) {
                                403 -> """{"code":"place.not_editable"}"""
                                409 -> """{"code":"place.conflict"}"""
                                422 -> """{"code":"validation.name.too_long"}"""
                                else -> """{"code":"FUTURE_ERROR"}"""
                            }
                        respond(error, HttpStatusCode.fromValue(status))
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                val api = placesClient(client, ApiConfig("https://example.test"))
                statuses.forEach { apiFailure(api.save("custom", place().toSaveRequest())) }
                assertEquals(statuses.size, calls)
            } finally {
                client.close()
            }
        }
}
