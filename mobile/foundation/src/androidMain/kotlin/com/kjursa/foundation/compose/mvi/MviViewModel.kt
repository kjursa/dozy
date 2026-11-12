package com.kjursa.foundation.compose.mvi

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Base for a simple unidirectional flow: actions update state or emit one-off effects. */
abstract class MviViewModel<State : Any, Action : Any, Effect : Any>(
    initialState: State,
    private val statePersistence: StatePersistence<State>? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(statePersistence?.restore(initialState) ?: initialState)
    val uiState: StateFlow<State> = mutableState.asStateFlow()

    private val effectChannel = Channel<Effect>(capacity = Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** Entry point called by the UI when a user action occurs. */
    fun dispatch(action: Action) = onAction(action)

    protected abstract fun onAction(action: Action)

    /** Call on the main thread. Reducers are pure; persistence runs after the state update commits. */
    @MainThread
    protected fun updateState(reducer: State.() -> State) {
        mutableState.update(reducer)
        statePersistence?.save(mutableState.value)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    override fun onCleared() {
        effectChannel.close()
        super.onCleared()
    }
}
