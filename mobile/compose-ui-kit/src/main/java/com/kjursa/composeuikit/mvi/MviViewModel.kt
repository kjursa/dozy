package com.kjursa.composeuikit.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Base for a simple unidirectional flow: intents update state or emit one-off effects. */
abstract class MviViewModel<State : Any, Intent : Any, Effect : Any>(initialState: State) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<State> = mutableState.asStateFlow()

    private val effectChannel = Channel<Effect>(capacity = Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /** Entry point called by the UI when a user action occurs. */
    fun dispatch(intent: Intent) = onIntent(intent)

    protected abstract fun onIntent(intent: Intent)

    protected fun updateState(reducer: State.() -> State) {
        mutableState.update(reducer)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    override fun onCleared() {
        effectChannel.close()
        super.onCleared()
    }
}
