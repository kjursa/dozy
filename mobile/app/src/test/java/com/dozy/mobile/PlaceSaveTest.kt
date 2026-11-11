package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.KtorPlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.dozy.mobile.feature.place.data.api.PlacesDto
import com.dozy.mobile.feature.place.data.toDto
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaceSaveTest {
    @Test fun savesBeforeHttpAndRetriesDurablePendingRowsWithSameId() =
        runTest {
            val dao = FakePlaceDao()
            var available = false
            val ids = mutableListOf<String>()
            val api =
                object : PlaceApi {
                    override suspend fun list() = PlacesDto(emptyList())

                    override suspend fun save(place: PlaceDto): Boolean {
                        assertEquals(place.name, dao.find(place.id)?.name)
                        ids += place.id
                        return available
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
                object : PlaceApi {
                    override suspend fun list(): PlacesDto? = null

                    override suspend fun save(place: PlaceDto): Boolean {
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
            val dto = place("custom/id").toDto().copy(inhabitants = "Bees")
            val client =
                HttpClient(
                    MockEngine { request ->
                        calls++
                        assertEquals("PUT", request.method.value)
                        assertEquals("/places/custom%2Fid", request.url.encodedPath)
                        val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                        assertEquals(dto, Json.decodeFromString<PlaceDto>(body))
                        respond("", if (calls == 1) HttpStatusCode.NoContent else HttpStatusCode.ServiceUnavailable)
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                assertFalse(KtorPlaceApi(client, ApiConfig(null)).save(dto))
                assertEquals(0, calls)
                val api = KtorPlaceApi(client, ApiConfig("https://example.test/"))
                assertTrue(api.save(dto))
                assertFalse(api.save(dto))
            } finally {
                client.close()
            }
        }
}
