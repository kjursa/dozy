package com.dozy.mobile

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.place.v1.PlacesCreateResult
import com.dozy.api.client.place.v1.PlacesListResult
import com.dozy.api.contracts.v1.place.CreatedPlaceResponse
import com.dozy.api.contracts.v1.place.PlacesListError
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.dozy.mobile.feature.place.data.api.PlacesDto
import com.dozy.mobile.feature.place.data.toDomain
import com.dozy.mobile.feature.place.data.toDto
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceInput
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
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class PlaceDataTest {
    @Test fun cachesTheServerIdOnlyAfterCreationSucceeds() =
        runTest {
            val dao = FakePlaceDao()
            val input = (PlaceInput.validate("  Garden  ", " Quiet ", "Bees") as PlaceInput.Validation.Valid).input
            var result: PlacesCreateResult = PlacesCreateResult.TechnicalError(TechnicalError.Server(503))
            val api =
                fakePlacesClient(create = { request ->
                    assertTrue(dao.rows.value.isEmpty())
                    assertEquals("Garden", request.name)
                    assertEquals("Bees", request.inhabitants)
                    result
                }) { error("Creation must not list places") }
            val repository = DefaultPlaceRepository(dao, fakePlacesClients(api), emptyList())
            assertFailsWith<IllegalStateException> { repository.create(input) }
            assertTrue(dao.rows.value.isEmpty())
            val imageUrl = "https://example.test/api/v1/place-icons/cottage.png"
            result = PlacesCreateResult.Success(CreatedPlaceResponse("server-place", imageUrl))
            val place = repository.create(input)
            assertEquals(Place.Id("server-place"), place.id)
            assertEquals(imageUrl, place.imageUrl)
            assertEquals(place, repository.observe().first().single())
            assertFalse(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
        }

    @Test fun localReadsMergeFallbacksWithoutHttpAndOrderCustomFirst() =
        runTest {
            val dao = FakePlaceDao()
            dao.upsert(place("z", "Zoo").toEntity())
            dao.upsert(place("a", "Attic").toEntity())
            val bundled = listOf(place("place-00000000-0000-7000-8000-000000000001", "Woods", false))
            val repository =
                DefaultPlaceRepository(dao, fakePlacesClients(fakePlacesClient { error("No network during observation") }), bundled)
            val ordered = ObservePlaces(repository)().first()
            assertEquals(listOf("a", "z", "place-00000000-0000-7000-8000-000000000001"), ordered.map { it.id.value })
            assertEquals(listOf(true, true, false), ordered.map { it.canEdit })
            assertEquals(
                ordered,
                ObservePlaces(
                    DefaultPlaceRepository(
                        dao,
                        fakePlacesClients(
                            fakePlacesClient {
                                PlacesListResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))
                            },
                        ),
                        bundled,
                    ),
                )().first(),
            )
            dao.rows.value = emptyList()
            assertEquals(bundled, repository.observe().first())
            assertEquals(
                emptyList<Any>(),
                DefaultPlaceRepository(
                    dao,
                    fakePlacesClients(
                        fakePlacesClient {
                            PlacesListResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))
                        },
                    ),
                    emptyList(),
                ).observe().first(),
            )
            assertFalse(
                DefaultPlaceRepository(
                    dao,
                    fakePlacesClients(
                        fakePlacesClient {
                            PlacesListResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))
                        },
                    ),
                    bundled,
                ).refresh(),
            )
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
            val repository =
                DefaultPlaceRepository(dao, fakePlacesClients(fakePlacesClient { PlacesListResult.Success(snapshot) }), emptyList())
            assertTrue(repository.refresh())
            assertEquals(
                "Local edit",
                repository
                    .find(
                        Place
                            .Id("same"),
                    )?.name
                    ?.value,
            )
            assertEquals(2, repository.observe().first().size)
            snapshot = PlacesDto(emptyList())
            repository.refresh()
            assertEquals(listOf("same"), repository.observe().first().map { it.id.value })
        }

    @Test fun conditionalRefreshKeepsCachedPlacesAndDropsMissingSyncedPlaces() =
        runTest {
            val dao = FakePlaceDao()
            val place = place()
            dao.upsert(place.toEntity().copy(pendingSync = false))
            val remote = PlacesDto(listOf(place.toDto()))
            val tag = "W/\"places\""
            var response: PlacesListResult = PlacesListResult.Success(remote, tag)
            val requests = mutableListOf<String?>()
            val clients =
                fakePlacesClients(
                    fakePlacesClient {
                        requests += it
                        response
                    },
                )
            assertTrue(DefaultPlaceRepository(dao, clients, emptyList()).refresh())
            val restored = DefaultPlaceRepository(dao, clients, emptyList())
            response = PlacesListResult.NotModified
            assertTrue(restored.refresh())
            assertEquals(place, restored.observe().first().single())
            response = PlacesListResult.Success(PlacesDto(emptyList()), "W/\"empty\"")
            assertTrue(restored.refresh())
            assertTrue(restored.observe().first().isEmpty())
            assertEquals(listOf(null, tag, tag), requests)
        }

    @Test fun typedRefreshFailurePreservesCachedPlaces() =
        runTest {
            val dao = FakePlaceDao()
            val existing = place().toEntity().copy(local = false, pendingSync = false)
            dao.upsert(existing)
            val failures: List<PlacesListResult> =
                listOf(
                    PlacesListResult.ApiError(PlacesListError.Unknown("NEW_RULE")),
                    PlacesListResult.TechnicalError(TechnicalError.Server(503)),
                    PlacesListResult.TechnicalError(TechnicalError.Communication(IOException("Offline"))),
                )
            for (failure in failures) {
                val api = fakePlacesClient { failure }
                assertFalse(DefaultPlaceRepository(dao, fakePlacesClients(api), emptyList()).refresh())
                assertEquals(listOf(existing), dao.rows.value)
            }
        }

    @Test fun malformedSnapshotDoesNotReplaceCacheAndCancellationPropagates() =
        runTest {
            val dao = FakePlaceDao()
            val existing = place().toEntity()
            dao.upsert(existing)
            val repository =
                DefaultPlaceRepository(
                    dao,
                    fakePlacesClients(
                        fakePlacesClient { PlacesListResult.Success(PlacesDto(listOf(PlaceDto("x", "", "desc", "USER", true)))) },
                    ),
                    emptyList(),
                )
            try {
                repository.refresh()
                fail("Expected invalid transport")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals(listOf(existing), dao.rows.value)
            try {
                DefaultPlaceRepository(dao, fakePlacesClients(fakePlacesClient { throw CancellationException() }), emptyList()).refresh()
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
                val result = apiValue(placesClient(client, ApiConfig("https://example.test/")).list()).places
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
                        respond("""{"code":"UNAVAILABLE"}""", HttpStatusCode.ServiceUnavailable)
                    },
                )
            try {
                assertTrue(apiFailure(placesClient(client, ApiConfig(null)).list()) is TechnicalError.Communication)
                assertEquals(0, calls)
                assertEquals(TechnicalError.Server(503), apiFailure(placesClient(client, ApiConfig("https://example.test")).list()))
                assertEquals(1, calls)
            } finally {
                client.close()
            }
        }
}
