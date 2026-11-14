package com.dozy.features.place.application

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PlaceServiceTest {
    private val installation = InstallationId("installation")
    private val command = (SavePlace.parse("Woods", "Forest", null) as ParseResult.Valid).value

    @Test
    fun `orders custom places first with deterministic name and ID ties`() = runBlocking {
        val repository = FakeRepository(listOf(
            place("builtin:woods", "A", Place.Ownership.PREDEFINED),
            place("c", "Zoo"), place("b", "alpha"), place("a", "Alpha"),
        ))
        assertEquals(listOf("a", "b", "c", "builtin:woods"),
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
        val id = placeId("custom")
        assertEquals(SavePlaceResult.Conflict, PlaceService(repository).save(installation, id, command))
        assertEquals(Triple(installation, id, command), repository.saved)
    }

    private fun place(id: String, name: String, ownership: Place.Ownership = Place.Ownership.CUSTOM) =
        Place(placeId(id), placeName(name), placeDescription("Forest"), null, ownership)

    private fun placeId(value: String) = Place.Id(value)
    private fun placeName(value: String) = Place.Name(value)
    private fun placeDescription(value: String) = Place.Description(value)

    private class FakeRepository(private val places: List<Place> = emptyList()) : PlaceRepository {
        var lastInstallation: InstallationId? = null
        var saveCalls = 0
        var saved: Triple<InstallationId, Place.Id, SavePlace>? = null
        override suspend fun list(installation: InstallationId): List<Place> {
            lastInstallation = installation
            return places
        }
        override suspend fun save(installation: InstallationId, id: Place.Id, command: SavePlace): SavePlaceResult {
            saveCalls++
            saved = Triple(installation, id, command)
            return SavePlaceResult.Conflict
        }
    }
}
