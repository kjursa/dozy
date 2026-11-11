package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.DeleteCharacter
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacter
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsAction
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsEffect
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeCharacterRepository()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(id: String = "2") =
        CharacterDetailsViewModel(ObserveCharacter(repository), DeleteCharacter(repository), Character.Id(id))

    @Test fun deleteUseCaseMapsFailureAndPreservesCancellation() =
        runTest(dispatcher) {
            val operation = DeleteCharacter(repository)
            repository.failure = IllegalStateException("disk")
            assertEquals(DeleteCharacter.Result.Failure, operation(Character.Id("1")))
            val cancellation = kotlinx.coroutines.CancellationException("cancelled")
            repository.failure = cancellation
            try {
                operation(Character.Id("1"))
                fail("Expected cancellation")
            } catch (actual: kotlinx.coroutines.CancellationException) {
                assertSame(cancellation, actual)
            }
            assertEquals(3, repository.characters.value.size)
        }

    @Test fun observesDomainIdAndMissingIdIsUnavailable() =
        runTest(dispatcher) {
            val details = vm()
            assertTrue(details.uiState.value.loading)
            advanceUntilIdle()
            assertEquals(character("2", "Luna"), details.uiState.value.character)
            val missing = vm("missing")
            advanceUntilIdle()
            assertNull(missing.uiState.value.character)
            assertNull(missing.uiState.value.readError)
            assertFalse(missing.uiState.value.loading)
            repository.characters.value = listOf(character("2", "Updated"))
            advanceUntilIdle()
            assertEquals(
                "Updated",
                details.uiState.value.character
                    ?.details
                    ?.name
                    ?.value,
            )
        }

    @Test fun cancelAndBackDismissConfirmationWithoutDeleting() =
        runTest(dispatcher) {
            val details = vm()
            advanceUntilIdle()
            details.dispatch(CharacterDetailsAction.Delete)
            assertTrue(details.uiState.value.deleteDialog)
            details.dispatch(CharacterDetailsAction.CancelDelete)
            assertFalse(details.uiState.value.deleteDialog)
            details.dispatch(CharacterDetailsAction.ConfirmDelete)
            details.dispatch(CharacterDetailsAction.Delete)
            details.dispatch(CharacterDetailsAction.Back)
            advanceUntilIdle()
            assertFalse(details.uiState.value.deleteDialog)
            assertEquals(0, repository.deletes)
            assertEquals(3, repository.characters.value.size)
        }

    @Test fun confirmationDeletesOnlySelectedIdAndEmitsOneReturnDespiteRepeatedClicks() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            repository.beforeDelete = { gate.await() }
            val details = vm()
            advanceUntilIdle()
            details.dispatch(CharacterDetailsAction.Delete)
            val effect = async { details.effects.first() }
            repeat(3) { details.dispatch(CharacterDetailsAction.ConfirmDelete) }
            advanceUntilIdle()
            assertTrue(details.uiState.value.deleting)
            details.dispatch(CharacterDetailsAction.CancelDelete)
            details.dispatch(CharacterDetailsAction.Back)
            assertTrue(details.uiState.value.deleteDialog)
            assertEquals(1, repository.deletes)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(CharacterDetailsEffect.Deleted, effect.await())
            assertEquals(listOf("1", "3"), repository.characters.value.map { it.id.value })
        }

    @Test fun failureKeepsCharacterAndDialogAndAllowsRetry() =
        runTest(dispatcher) {
            repository.failure = IllegalStateException("network")
            val details = vm()
            advanceUntilIdle()
            details.dispatch(CharacterDetailsAction.Delete)
            details.dispatch(CharacterDetailsAction.ConfirmDelete)
            advanceUntilIdle()
            assertNotNull(details.uiState.value.deleteError)
            assertNotNull(details.uiState.value.character)
            assertFalse(details.uiState.value.deleting)
            repository.failure = null
            details.dispatch(CharacterDetailsAction.ConfirmDelete)
            advanceUntilIdle()
            assertEquals(CharacterDetailsEffect.Deleted, details.effects.first())
            assertEquals(2, repository.deletes)
        }

    @Test fun navigationUsesIdAndReturnsToOriginThroughBackEffect() =
        runTest(dispatcher) {
            val details = vm()
            advanceUntilIdle()
            details.dispatch(CharacterDetailsAction.Story)
            details.dispatch(CharacterDetailsAction.Story)
            advanceUntilIdle()
            assertEquals(CharacterDetailsEffect.Story(Character.Id("2")), details.effects.first())
            details.resumed()
            details.dispatch(CharacterDetailsAction.Edit)
            advanceUntilIdle()
            assertEquals(CharacterDetailsEffect.Edit(Character.Id("2")), details.effects.first())
            details.resumed()
            details.dispatch(CharacterDetailsAction.Back)
            advanceUntilIdle()
            assertEquals(CharacterDetailsEffect.Back, details.effects.first())
        }

    @Test fun readFailureIsDistinctFromMissingAndRetryResubscribes() =
        runTest(dispatcher) {
            var fail = true
            val source =
                object : com.dozy.mobile.feature.character.domain.repository.CharacterRepository by repository {
                    override fun observe(id: Character.Id) =
                        flow {
                            if (fail) error("disk")
                            emit(character(id.value))
                        }
                }
            val details = CharacterDetailsViewModel(ObserveCharacter(source), DeleteCharacter(source), Character.Id("1"))
            advanceUntilIdle()
            assertNotNull(details.uiState.value.readError)
            fail = false
            details.dispatch(CharacterDetailsAction.Retry)
            advanceUntilIdle()
            assertNull(details.uiState.value.readError)
            assertEquals(character(), details.uiState.value.character)
        }
}
