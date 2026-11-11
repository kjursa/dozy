package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.PlaceIdGenerator
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.place.presentation.form.PlaceFormAction
import com.dozy.mobile.feature.place.presentation.form.PlaceFormEffect
import com.dozy.mobile.feature.place.presentation.form.PlaceFormUiState
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(
        repository: FakePlaceRepository,
        handle: SavedStateHandle = SavedStateHandle(),
        id: String? = null,
    ) = PlaceFormViewModel(
        SavedStatePersistence(handle, PlaceFormUiState.serializer()),
        SavePlace(
            repository,
            PlaceIdGenerator {
                Place.Id("new")
            },
        ),
        SelectPlace(repository),
        id,
    )

    @Test fun validatesWithoutWritingThenKeepsInputOnFailureAndRetriesOnlyOnce() =
        runTest(dispatcher) {
            val repository = FakePlaceRepository()
            val handle = SavedStateHandle()
            val viewModel = vm(repository, handle)
            viewModel.dispatch(PlaceFormAction.Save)
            assertNotNull(viewModel.uiState.value.nameError)
            assertNotNull(viewModel.uiState.value.descriptionError)
            assertEquals(0, repository.saves)
            viewModel.dispatch(PlaceFormAction.Name("Garden"))
            viewModel.dispatch(PlaceFormAction.Description("Sunny"))
            repository.failure = true
            viewModel.dispatch(PlaceFormAction.Save)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.saving)
            assertNotNull(viewModel.uiState.value.error)
            assertEquals("Garden", viewModel.uiState.value.name)
            val restored =
                vm(
                    repository,
                    SavedStateHandle(
                        mapOf(
                            SavedStatePersistence.DEFAULT_KEY to handle.get<String>(SavedStatePersistence.DEFAULT_KEY),
                        ),
                    ),
                )
            assertEquals("Garden", restored.uiState.value.name)
            assertNull(restored.uiState.value.error)
            repository.failure = false
            val effect = async { restored.effects.first() }
            restored.dispatch(PlaceFormAction.Save)
            restored.dispatch(PlaceFormAction.Save)
            advanceUntilIdle()
            assertEquals(PlaceFormEffect.Saved(Place.Id("new")), effect.await())
            assertEquals(2, repository.saves)
        }

    @Test fun editsLoadOnceRestoreUnsavedInputAndBackDoesNotSave() =
        runTest(dispatcher) {
            val repository = FakePlaceRepository()
            val handle = SavedStateHandle()
            val viewModel = vm(repository, handle, "custom")
            advanceUntilIdle()
            assertEquals("Garden", viewModel.uiState.value.name)
            viewModel.dispatch(PlaceFormAction.Name("Changed"))
            val restored =
                vm(
                    repository,
                    SavedStateHandle(
                        mapOf(
                            SavedStatePersistence.DEFAULT_KEY to handle.get<String>(SavedStatePersistence.DEFAULT_KEY),
                        ),
                    ),
                    "custom",
                )
            advanceUntilIdle()
            assertEquals("Changed", restored.uiState.value.name)
            val effect = async { restored.effects.first() }
            restored.dispatch(PlaceFormAction.Back)
            advanceUntilIdle()
            assertEquals(PlaceFormEffect.Back, effect.await())
            assertEquals(0, repository.saves)
            val predefined = vm(repository, id = "builtin:woods")
            advanceUntilIdle()
            assertFalse(predefined.uiState.value.initialized)
            predefined.dispatch(PlaceFormAction.Save)
            assertEquals(0, repository.saves)
        }

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
            assertEquals(UiText.Resource(R.string.place_unavailable), missing.uiState.value.error)
            assertEquals(UiText.Resource(R.string.place_not_editable), predefined.uiState.value.error)
            missing.dispatch(PlaceFormAction.Save)
            predefined.dispatch(PlaceFormAction.Save)
            assertEquals(0, repository.saves)
            repository.findFailure = true
            val retry = vm(repository, id = "custom")
            advanceUntilIdle()
            assertEquals(UiText.Resource(R.string.place_load_error), retry.uiState.value.error)
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
            assertEquals(UiText.Resource(R.string.place_unavailable), retry.uiState.value.error)
            assertFalse(retry.uiState.value.saving)
            assertEquals(0, repository.saves)
        }
}
