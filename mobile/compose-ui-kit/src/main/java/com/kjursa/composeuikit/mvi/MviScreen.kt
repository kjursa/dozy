package com.kjursa.composeuikit.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect

/** Collects an MVI ViewModel's state and effects, then connects state/actions to stateless UI. */
@Composable
fun <State : Any, Action : Any, Effect : Any> MviScreen(
    viewModel: MviViewModel<State, Action, Effect>,
    onEffect: suspend (Effect) -> Unit,
    content: @Composable (state: State, dispatch: (Action) -> Unit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> currentOnEffect(effect) }
    }

    content(uiState, viewModel::dispatch)
}
