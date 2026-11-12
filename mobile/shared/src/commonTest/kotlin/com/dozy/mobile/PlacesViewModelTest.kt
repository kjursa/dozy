package com.dozy.mobile

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.place.presentation.list.PlacesAction
import com.dozy.mobile.feature.place.presentation.list.PlacesEffect
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(repository: FakePlaceRepository) =
        PlacesViewModel(ObservePlaces(repository), RefreshPlaces(repository), SelectPlace(repository))

    @Test fun loadingFailureRetainsPlacesRetryIsSingleFlightAndCanRecover() =
        runTest(dispatcher) {
            val repository = FakePlaceRepository()
            val gate = CompletableDeferred<Boolean>()
            repository.refreshBlock = { gate.await() }
            val viewModel = vm(repository)
            assertTrue(viewModel.uiState.value.loading)
            viewModel.dispatch(PlacesAction.Retry)
            advanceUntilIdle()
            assertEquals(1, repository.refreshCalls)
            assertEquals(2, viewModel.uiState.value.places.size)
            gate.completeExceptionally(IllegalStateException("offline"))
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.loading)
            assertNotNull(viewModel.uiState.value.error)
            assertEquals(2, viewModel.uiState.value.places.size)
            repository.refreshBlock = { true }
            viewModel.dispatch(PlacesAction.Retry)
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.error)
            assertEquals(2, repository.refreshCalls)
        }

    @Test fun noCustomStillShowsPredefinedAndTrueEmptyIsRepresentable() =
        runTest(dispatcher) {
            val repository = FakePlaceRepository()
            repository.places.value = listOf(place("builtin:woods", custom = false))
            val viewModel = vm(repository)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasCustom)
            assertEquals(1, viewModel.uiState.value.places.size)
            repository.places.value = emptyList()
            advanceUntilIdle()
            assertTrue(
                viewModel.uiState.value.places
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.loading)
        }

    @Test fun selectionAndNavigationPreventDuplicateClicksAndRejectPredefinedEditing() =
        runTest(dispatcher) {
            val viewModel = vm(FakePlaceRepository())
            advanceUntilIdle()
            viewModel.dispatch(PlacesAction.Edit(Place.Id("builtin:woods")))
            assertFalse(viewModel.uiState.value.navigating)
            val effect = async { viewModel.effects.first() }
            viewModel.dispatch(PlacesAction.Select(Place.Id("custom")))
            viewModel.dispatch(PlacesAction.Select(Place.Id("custom")))
            viewModel.dispatch(PlacesAction.Add)
            advanceUntilIdle()
            assertEquals(PlacesEffect.Selected(Place.Id("custom")), effect.await())
            assertTrue(viewModel.uiState.value.navigating)
            viewModel.navigationHandled()
            val back = async { viewModel.effects.first() }
            viewModel.dispatch(PlacesAction.Back)
            advanceUntilIdle()
            assertEquals(PlacesEffect.Back, back.await())
        }

    @Test fun formSurpriseAndUnavailableSelectionHaveExplicitOutcomes() =
        runTest(dispatcher) {
            val viewModel = vm(FakePlaceRepository())
            advanceUntilIdle()
            viewModel.dispatch(PlacesAction.Select(Place.Id("missing")))
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.navigating)
            for ((action, expected) in listOf(
                PlacesAction.Add to PlacesEffect.Form(null),
                PlacesAction.Edit(Place.Id("custom")) to PlacesEffect.Form(Place.Id("custom")),
                PlacesAction.Surprise to PlacesEffect.Selected(null),
            )) {
                viewModel.navigationHandled()
                val result = async { viewModel.effects.first() }
                viewModel.dispatch(action)
                advanceUntilIdle()
                assertEquals(expected, result.await())
            }
        }
}
