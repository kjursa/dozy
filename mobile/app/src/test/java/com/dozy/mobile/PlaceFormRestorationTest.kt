package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.presentation.form.PlaceFormAction
import com.dozy.mobile.feature.place.presentation.form.PlaceFormEffect
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlaceFormRestorationTest : PlaceFormViewModelFixture() {
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
}
