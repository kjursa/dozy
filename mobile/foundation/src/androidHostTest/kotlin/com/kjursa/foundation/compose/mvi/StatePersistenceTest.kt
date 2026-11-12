package com.kjursa.foundation.compose.mvi

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StatePersistenceTest {
    @Test fun noSnapshotUsesProvidedInitialState() {
        val fallback = TestState(text = "initial")
        val store = SavedStatePersistence(SavedStateHandle(), TestState.serializer())
        assertSame(fallback, store.restore(fallback))
    }

    @Test fun viewModelAutomaticallyPersistsUpdatesAndRestoresOnlySerializableFields() {
        val handle = SavedStateHandle()
        val vm = TestViewModel(SavedStatePersistence(handle, TestState.serializer()))
        vm.dispatch("first")
        vm.dispatch("second")
        assertEquals(setOf(SavedStatePersistence.DEFAULT_KEY), handle.keys())
        assertEquals(true, vm.uiState.value.busy)
        val snapshot = requireNotNull(handle.get<String>(SavedStatePersistence.DEFAULT_KEY))
        assertFalse(snapshot.contains("busy"))
        assertFalse(snapshot.contains("message"))
        val recreated =
            TestViewModel(
                SavedStatePersistence(
                    SavedStateHandle(mapOf(SavedStatePersistence.DEFAULT_KEY to snapshot)),
                    TestState.serializer(),
                ),
            )
        assertEquals("second", recreated.uiState.value.text)
        assertEquals(2, recreated.uiState.value.count)
        assertFalse(recreated.uiState.value.busy)
        assertNull(recreated.uiState.value.message)
    }

    @Test fun persistenceRunsAfterTheNewStateIsPublished() {
        lateinit var vm: TestViewModel
        val saved = mutableListOf<TestState>()
        val store =
            object : StatePersistence<TestState> {
                override fun restore(defaultState: TestState) = defaultState

                override fun save(state: TestState) {
                    assertEquals(state, vm.uiState.value)
                    saved += state
                }
            }
        vm = TestViewModel(store)
        vm.dispatch("committed")
        assertEquals(listOf(vm.uiState.value), saved)
    }

    @Test fun oldSnapshotsIgnoreRemovedFieldsAndDefaultNewFields() {
        val handle = SavedStateHandle(mapOf(SavedStatePersistence.DEFAULT_KEY to """{"text":"legacy","removed":true}"""))
        val state = SavedStatePersistence(handle, TestState.serializer()).restore(TestState())
        assertEquals("legacy", state.text)
        assertEquals(0, state.count)
    }

    @Test fun invalidSnapshotsAreDiscardedInsteadOfPreventingScreenCreation() {
        for (encoded in listOf("not json", """{"count":"wrong type"}""")) {
            val handle = SavedStateHandle(mapOf(SavedStatePersistence.DEFAULT_KEY to encoded))
            val fallback = TestState(text = "fallback")
            assertSame(fallback, SavedStatePersistence(handle, TestState.serializer()).restore(fallback))
            assertFalse(handle.contains(SavedStatePersistence.DEFAULT_KEY))
        }
    }

    @Test fun separateKeysPreserveIndependentStatesAndNavigationArguments() {
        val handle = SavedStateHandle(mapOf("id" to "route-argument"))
        SavedStatePersistence(handle, TestState.serializer(), key = "first").save(TestState(text = "one"))
        SavedStatePersistence(handle, TestState.serializer(), key = "second").save(TestState(text = "two"))
        assertEquals("one", SavedStatePersistence(handle, TestState.serializer(), key = "first").restore(TestState()).text)
        assertEquals("two", SavedStatePersistence(handle, TestState.serializer(), key = "second").restore(TestState()).text)
        assertEquals("route-argument", handle.get<String>("id"))
    }

    @Serializable
    private data class TestState(
        val text: String = "",
        val count: Int = 0,
        @Transient val busy: Boolean = false,
        @Transient val message: Any? = null,
    )

    private class TestViewModel(
        store: StatePersistence<TestState>,
    ) : MviViewModel<TestState, String, Unit>(TestState(), store) {
        override fun onAction(action: String) {
            updateState { copy(text = action, count = count + 1, busy = true, message = Any()) }
        }
    }
}
