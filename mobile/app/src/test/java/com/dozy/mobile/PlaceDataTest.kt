package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.KtorPlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.dozy.mobile.feature.place.data.api.PlacesDto
import com.dozy.mobile.feature.place.data.toDomain
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaceDataTest {
    @Test fun localReadsMergeFallbacksWithoutHttpAndOrderCustomFirst() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place("z", "Zoo").toEntity())
            dao.upsert(place("a", "Attic").toEntity())
            val bundled = listOf(place("builtin:woods", "Woods", false))
            val repository = DefaultPlaceRepository(dao, PlaceApi { error("No network during observation") }, bundled)
            val ordered = ObservePlaces(repository)().first()
            assertEquals(listOf("a", "z", "builtin:woods"), ordered.map { it.id.value })
            assertEquals(listOf(true, true, false), ordered.map { it.canEdit })
            assertEquals(ordered, ObservePlaces(DefaultPlaceRepository(dao, PlaceApi { null }, bundled))().first())
            dao.rows.value = emptyList()
            assertEquals(bundled, repository.observe().first())
            assertEquals(emptyList<Any>(), DefaultPlaceRepository(dao, PlaceApi { null }, emptyList()).observe().first())
            assertFalse(DefaultPlaceRepository(dao, PlaceApi { null }, bundled).refresh())
        }

    @Test fun refreshReplacesRemoteSnapshotAtomicallyAndProtectsLocalEdits() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place("same", "Local edit").toEntity())
            var snapshot =
                PlacesDto(
                    listOf(
                        PlaceDto("same", "Server", "Old description", "CUSTOM", true),
                        PlaceDto("remote", "Island", "Sandy", "PREDEFINED", false),
                    ),
                )
            val repository = DefaultPlaceRepository(dao, PlaceApi { snapshot }, emptyList())
            assertTrue(repository.refresh())
            assertEquals(
                "Local edit",
                repository
                    .find(
                        com.dozy.mobile.feature.place.domain.model.Place
                            .Id("same"),
                    )?.name
                    ?.value,
            )
            assertEquals(2, repository.observe().first().size)
            snapshot = PlacesDto(emptyList())
            repository.refresh()
            assertEquals(listOf("same"), repository.observe().first().map { it.id.value })
        }

    @Test fun malformedSnapshotDoesNotReplaceCacheAndCancellationPropagates() =
        runTest {
            val dao = FakePlaceDao()
            val existing = place().toEntity()
            dao.upsert(existing)
            val repository =
                DefaultPlaceRepository(dao, PlaceApi { PlacesDto(listOf(PlaceDto("x", "", "desc", "CUSTOM", true))) }, emptyList())
            try {
                repository.refresh()
                fail("Expected invalid transport")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals(listOf(existing), dao.rows.value)
            try {
                DefaultPlaceRepository(dao, PlaceApi { throw CancellationException() }, emptyList()).refresh()
                fail("Expected cancellation")
            } catch (
                _: CancellationException,
            ) {
            }
        }

    @Test fun mappingKeepsLegacyValuesAndOptionalInhabitantsAndRejectsEditablePredefined() {
        val dto = PlaceDto("old", "x".repeat(90), "Old description", "CUSTOM", false, "An owl")
        val place = dto.toEntity().toDomain()
        assertEquals(dto.name, place.name.value)
        assertEquals("An owl", place.inhabitants?.value)
        assertFalse(place.canEdit)
        assertNull(
            dto
                .copy(inhabitants = " ")
                .toEntity()
                .toDomain()
                .inhabitants,
        )
        assertThrows(IllegalArgumentException::class.java) { dto.copy(ownership = "PREDEFINED", editable = true).toEntity() }
        assertThrows(IllegalArgumentException::class.java) { dto.copy(ownership = "unknown").toEntity() }
    }

    @Test fun ktorUsesGetAndDecodesBothSourcesWithOptionalInhabitants() =
        runTest {
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("GET", request.method.value)
                        assertEquals("https://example.test/places", request.url.toString())
                        respond(
                            """{"places":[{"id":"one","name":"Garden","description":"Sunny","ownership":"CUSTOM","editable":true,"inhabitants":"Bees"},{"id":"two","name":"Woods","description":"Quiet","ownership":"PREDEFINED","editable":false}]}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                val result = requireNotNull(KtorPlaceApi(client, ApiConfig("https://example.test/")).list())
                assertEquals(2, result.places.size)
                assertEquals("Bees", result.places.first().inhabitants)
                assertNull(result.places.last().inhabitants)
                assertEquals(listOf(true, false), result.places.map { it.toEntity().toDomain().canEdit })
            } finally {
                client.close()
            }
        }

    @Test fun disabledApiMakesNoRequestAndNon200ResponsesFail() =
        runTest {
            var calls = 0
            val client =
                HttpClient(
                    MockEngine {
                        calls++
                        respond("", HttpStatusCode.ServiceUnavailable)
                    },
                )
            try {
                assertNull(KtorPlaceApi(client, ApiConfig(null)).list())
                assertEquals(0, calls)
                try {
                    KtorPlaceApi(client, ApiConfig("https://example.test")).list()
                    fail("Expected failure")
                } catch (
                    _: IllegalStateException,
                ) {
                }
                assertEquals(1, calls)
            } finally {
                client.close()
            }
        }
}
