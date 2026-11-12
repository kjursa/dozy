package com.dozy.mobile

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceErrors
import com.dozy.mobile.feature.place.domain.model.PlaceInput
import com.dozy.mobile.feature.place.domain.usecase.PlaceIdGenerator
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class PlaceDomainTest {
    @Test fun validatesAllFieldsAndNormalizesOptionalInput() {
        val invalid = PlaceInput.validate(" ", "x".repeat(501), "x".repeat(501)) as PlaceInput.Validation.Invalid
        assertEquals(PlaceErrors(PlaceErrors.Error.REQUIRED, PlaceErrors.Error.TOO_LONG, PlaceErrors.Error.TOO_LONG), invalid.errors)
        val valid = (PlaceInput.validate("  Garden  ", " Sunny ", "  ") as PlaceInput.Validation.Valid).input
        assertEquals("Garden", valid.name.value)
        assertEquals("Sunny", valid.description.value)
        assertNull(valid.inhabitants)
        assertTrue(PlaceInput.validate("n".repeat(80), "d".repeat(500), "i".repeat(500)) is PlaceInput.Validation.Valid)
        assertTrue(PlaceInput.validate("n".repeat(81), "", "") is PlaceInput.Validation.Invalid)
    }

    @Test fun cannotEditPredefinedOrMissingPlaceAndSelectionDoesNotWrite() =
        runTest {
            val repository = FakePlaceRepository()
            val input = (PlaceInput.validate("Garden", "Sunny", "Bees") as PlaceInput.Validation.Valid).input
            val save = SavePlace(repository, PlaceIdGenerator { Place.Id("new") })
            assertEquals(SavePlace.Result.NotEditable, save(input, Place.Id("builtin:woods")))
            assertEquals(SavePlace.Result.Unavailable, save(input, Place.Id("missing")))
            assertEquals(0, repository.saves)
            assertTrue(SelectPlace(repository)(Place.Id("custom")) is SelectPlace.Result.Selected)
            assertEquals(0, repository.saves)
            val result = save(input, null) as SavePlace.Result.Saved
            assertTrue(result.place.canEdit)
            assertEquals("Bees", result.place.inhabitants?.value)
        }

    @Test fun refreshDistinguishesOfflineAndFailureAndPreservesCancellation() =
        runTest {
            val repository = FakePlaceRepository()
            val refresh = RefreshPlaces(repository)
            repository.refreshBlock = { false }
            assertEquals(RefreshPlaces.Result.Offline, refresh())
            repository.refreshBlock = { error("offline") }
            assertEquals(RefreshPlaces.Result.Failure, refresh())
            repository.refreshBlock = { throw CancellationException() }
            try {
                refresh()
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
        }
}
