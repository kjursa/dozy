package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.api.client.place.PlacesClient
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
import kotlin.test.*

class PlaceDataTest {
    @Test fun localReadsMergeFallbacksWithoutHttpAndOrderCustomFirst() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place("z", "Zoo").toEntity())
            dao.upsert(place("a", "Attic").toEntity())
            val bundled = listOf(place("builtin:woods", "Woods", false))
            val repository = DefaultPlaceRepository(dao, PlacesClient { error("No network during observation") }, bundled)
            val ordered = ObservePlaces(repository)().first()
            assertEquals(listOf("a", "z", "builtin:woods"), ordered.map { it.id.value })
            assertEquals(listOf(true, true, false), ordered.map { it.canEdit })
            assertEquals(ordered, ObservePlaces(DefaultPlaceRepository(dao, PlacesClient { throw com.dozy.api.client.ApiDisabledException() }, bundled))().first())
            dao.rows.value = emptyList()
            assertEquals(bundled, repository.observe().first())
            assertEquals(emptyList<Any>(), DefaultPlaceRepository(dao, PlacesClient { throw com.dozy.api.client.ApiDisabledException() }, emptyList()).observe().first())
            assertFalse(DefaultPlaceRepository(dao, PlacesClient { throw com.dozy.api.client.ApiDisabledException() }, bundled).refresh())
        }

    @Test fun refreshReplacesRemoteSnapshotAtomicallyAndProtectsLocalEdits() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place("same", "Local edit").toEntity())
            var snapshot =
                PlacesDto(
                    listOf(
                        PlaceDto("same", "Server", "Old description", "USER", true),
                        PlaceDto("remote", "Island", "Sandy", "SYSTEM", false),
                    ),
                )
            val repository = DefaultPlaceRepository(dao, PlacesClient { snapshot.places }, emptyList())
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
                DefaultPlaceRepository(dao, PlacesClient { listOf(PlaceDto("x", "", "desc", "USER", true)) }, emptyList())
            try {
                repository.refresh()
                fail("Expected invalid transport")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals(listOf(existing), dao.rows.value)
            try {
                DefaultPlaceRepository(dao, PlacesClient { throw CancellationException() }, emptyList()).refresh()
                fail("Expected cancellation")
            } catch (
                _: CancellationException,
            ) {
            }
        }

    @Test fun mappingKeepsLegacyValuesAndOptionalInhabitantsAndRejectsEditablePredefined() {
        val dto = PlaceDto("old", "x".repeat(90), "Old description", "USER", false, "An owl")
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
        assertFailsWith<IllegalArgumentException> { dto.copy(origin = "SYSTEM", editable = true).toEntity() }
        assertFailsWith<IllegalArgumentException> { dto.copy(origin = "unknown").toEntity() }
    }

    @Test fun ktorUsesGetAndDecodesBothSourcesWithOptionalInhabitants() =
        runTest {
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("GET", request.method.value)
                        assertEquals("https://example.test/api/v1/places", request.url.toString())
                        respond(
                            """{"places":[{"id":"one","name":"Garden","description":"Sunny","origin":"USER","editable":true,"inhabitants":"Bees"},{"id":"two","name":"Woods","description":"Quiet","origin":"SYSTEM","editable":false}]}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                val result = requireNotNull(placesClient(client, ApiConfig("https://example.test/")).list())
                assertEquals(2, result.size)
                assertEquals("Bees", result.first().inhabitants)
                assertNull(result.last().inhabitants)
                assertEquals(listOf(true, false), result.map { it.toEntity().toDomain().canEdit })
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
                assertFailsWith<com.dozy.api.client.ApiDisabledException> { placesClient(client, ApiConfig(null)).list() }
                assertEquals(0, calls)
                try {
                    placesClient(client, ApiConfig("https://example.test")).list()
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
