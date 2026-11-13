package com.dozy.features.place.application

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.PlaceInput
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.features.place.domain.repository.PlaceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PlaceServiceTest {
    private val installation = InstallationId("installation")
    private val input = requireNotNull(PlaceInput.from("Woods", "Forest", null))

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
            PlaceService(repository).save(installation, Place.Id("builtin:unknown"), input))
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `forwards input and repository outcome`() = runBlocking {
        val repository = FakeRepository()
        val id = Place.Id("custom")
        assertEquals(SavePlaceResult.Conflict, PlaceService(repository).save(installation, id, input))
        assertEquals(Triple(installation, id, input), repository.saved)
    }

    private fun place(id: String, name: String, ownership: Place.Ownership = Place.Ownership.CUSTOM) =
        Place(Place.Id(id), name, "Forest", null, ownership)

    private class FakeRepository(private val places: List<Place> = emptyList()) : PlaceRepository {
        var lastInstallation: InstallationId? = null
        var saveCalls = 0
        var saved: Triple<InstallationId, Place.Id, PlaceInput>? = null
        override suspend fun list(installation: InstallationId): List<Place> {
            lastInstallation = installation
            return places
        }
        override suspend fun save(installation: InstallationId, id: Place.Id, input: PlaceInput): SavePlaceResult {
            saveCalls++
            saved = Triple(installation, id, input)
            return SavePlaceResult.Conflict
        }
    }
}
