package com.dozy.features.place.application

import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceServiceTest {
    private val installation = OwnerId.generate()
    private val command = (SavePlace.parse("Woods", "Forest", null) as ParseResult.Valid).value

    @Test
    fun `orders custom places first with deterministic name and ID ties`() = runBlocking {
        val repository = FakeRepository(listOf(
            place("builtin:woods", "A", Place.Origin.SYSTEM),
            place("place-019a0000-0000-7000-8000-000000000003", "Zoo"),
            place("place-019a0000-0000-7000-8000-000000000002", "alpha"),
            place("place-019a0000-0000-7000-8000-000000000001", "Alpha"),
        ))
        assertEquals(listOf("place-019a0000-0000-7000-8000-000000000001", "place-019a0000-0000-7000-8000-000000000002", "place-019a0000-0000-7000-8000-000000000003", "builtin:woods"),
            PlaceService(repository).list(installation).map { it.id.value })
        assertEquals(installation, repository.lastInstallation)
    }

    @Test
    fun `rejects entire predefined namespace without touching repository`() = runBlocking {
        val repository = FakeRepository()
        assertEquals(SavePlaceResult.Forbidden,
            PlaceService(repository).save(installation, placeId("builtin:unknown"), command))
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `forwards input and repository outcome`() = runBlocking {
        val repository = FakeRepository()
        val id = PlaceId.generate()
        assertEquals(SavePlaceResult.Conflict, PlaceService(repository).save(installation, id, command))
        assertEquals(Triple(installation, id, command), repository.saved)
    }

    private fun place(id: String, name: String, origin: Place.Origin = Place.Origin.USER) =
        Place(placeId(id), placeName(name), placeDescription("Forest"), null, origin)

    private fun placeId(value: String) = requireNotNull(PlaceId.parse(value))
    private fun placeName(value: String) = Place.Name(value)
    private fun placeDescription(value: String) = Place.Description(value)

    private class FakeRepository(private val places: List<Place> = emptyList()) : PlaceRepository {
        var lastInstallation: OwnerId? = null
        var saveCalls = 0
        var saved: Triple<OwnerId, PlaceId, SavePlace>? = null
        override suspend fun list(installation: OwnerId): List<Place> {
            lastInstallation = installation
            return places
        }
        override suspend fun create(
            installation: OwnerId,
            command: SavePlace,
            requestKey: IdempotencyKey?
        ): SavePlaceResult = error("Unexpected creation")
        override suspend fun save(
            installation: OwnerId,
            id: PlaceId,
            command: SavePlace
        ): SavePlaceResult {
            saveCalls++
            saved = Triple(installation, id, command)
            return SavePlaceResult.Conflict
        }
    }
}
