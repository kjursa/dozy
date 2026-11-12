package com.kjursa.foundation.compose.mvi

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {
    @Test fun restoresBeforeActionsPersistsCommittedStateAndDeliversEffectsOnce() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            lateinit var vm: CounterViewModel
            val saved = mutableListOf<Int>()
            val persistence =
                object : StatePersistence<Int> {
                    override fun restore(defaultState: Int) = 10

                    override fun save(state: Int) {
                        assertEquals(state, vm.uiState.value)
                        saved += state
                    }
                }
            val owner = ViewModelStore()
            try {
                vm = CounterViewModel(persistence)
                owner.put("counter", vm)
                assertEquals(10, vm.uiState.value)
                vm.dispatch(Unit)
                advanceUntilIdle()
                assertEquals(listOf(11), saved)
                assertEquals(11, vm.effects.first())
                val next = backgroundScope.async { vm.effects.first() }
                advanceUntilIdle()
                assertFalse(next.isCompleted)
                vm.dispatch(Unit)
                advanceUntilIdle()
                assertEquals(12, next.await())
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    private class CounterViewModel(
        persistence: StatePersistence<Int>,
    ) : MviViewModel<Int, Unit, Int>(0, persistence) {
        override fun onAction(action: Unit) {
            updateState { this + 1 }
            sendEffect(uiState.value)
        }
    }
}
