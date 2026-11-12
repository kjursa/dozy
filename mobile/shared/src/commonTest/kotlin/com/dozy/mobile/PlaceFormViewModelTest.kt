package com.dozy.mobile

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.presentation.form.PlaceFormAction
import com.dozy.mobile.feature.place.presentation.form.PlaceFormEffect
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlaceFormViewModelTest : PlaceFormViewModelFixture() {
    @Test fun editClearsInhabitantsUpdatesSameIdAndEmitsOnlyOneSave() =
        runTest(dispatcher) {
            val repository = FakePlaceRepository()
            val original = place().copy(inhabitants = Place.Inhabitants("Bees"))
            repository.places.value = listOf(original)
            val viewModel = vm(repository, id = "custom")
            assertEquals("custom", viewModel.uiState.value.id)
            assertTrue(viewModel.uiState.value.loading)
            viewModel.dispatch(PlaceFormAction.Save)
            assertEquals(0, repository.saves)
            advanceUntilIdle()
            assertEquals("Bees", viewModel.uiState.value.inhabitants)
            viewModel.dispatch(PlaceFormAction.Inhabitants("  "))
            viewModel.dispatch(PlaceFormAction.Name("New garden"))
            val effect = async { viewModel.effects.first() }
            viewModel.dispatch(PlaceFormAction.Save)
            viewModel.dispatch(PlaceFormAction.Save)
            advanceUntilIdle()
            assertEquals(PlaceFormEffect.Saved(original.id), effect.await())
            viewModel.dispatch(PlaceFormAction.Save)
            assertEquals(1, repository.saves)
            val updated = repository.places.value.single()
            assertEquals(original.id, updated.id)
            assertEquals("New garden", updated.name.value)
            assertNull(updated.inhabitants)
            // Previously resolved story input is an immutable snapshot.
            assertEquals("Garden", original.name.value)
            assertEquals("Bees", original.inhabitants?.value)
        }

    @Test fun missingPermissionAndLoadFailureHaveDistinctErrorsAndRetryRecovers() =
        runTest(dispatcher) {
            val repository = FakePlaceRepository()
            val missing = vm(repository, id = "missing")
            val predefined = vm(repository, id = "builtin:woods")
            advanceUntilIdle()
            assertEquals(UiText.Resource(Res.string.place_unavailable), missing.uiState.value.error)
            assertEquals(UiText.Resource(Res.string.place_not_editable), predefined.uiState.value.error)
            missing.dispatch(PlaceFormAction.Save)
            predefined.dispatch(PlaceFormAction.Save)
            assertEquals(0, repository.saves)
            repository.findFailure = true
            val retry = vm(repository, id = "custom")
            advanceUntilIdle()
            assertEquals(UiText.Resource(Res.string.place_load_error), retry.uiState.value.error)
            assertFalse(retry.uiState.value.initialized)
            repository.findFailure = false
            retry.dispatch(PlaceFormAction.Retry)
            retry.dispatch(PlaceFormAction.Retry)
            advanceUntilIdle()
            assertEquals("Garden", retry.uiState.value.name)
            assertNull(retry.uiState.value.error)
            repository.places.value = emptyList()
            retry.dispatch(PlaceFormAction.Save)
            advanceUntilIdle()
            assertEquals(UiText.Resource(Res.string.place_unavailable), retry.uiState.value.error)
            assertFalse(retry.uiState.value.saving)
            assertEquals(0, repository.saves)
        }
}
