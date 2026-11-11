package com.dozy.mobile.feature.story.presentation.generation

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.R
import com.dozy.mobile.feature.story.domain.usecase.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class GenerationViewModel(
    observe: ObserveGenerations,
    private val refresh: RefreshGeneration,
    private val retry: RetryGeneration,
) : MviViewModel<GenerationUiState, GenerationAction, GenerationEffect>(GenerationUiState()) {
    init {
        viewModelScope.launch {
            observe()
                .catch { updateState { copy(loading = false, error = UiText.Resource(R.string.generation_unavailable)) } }
                .collect { updateState { copy(loading = false, jobs = it) } }
        }
    }

    override fun onAction(action: GenerationAction) {
        if (uiState.value.busy) return
        when (action) {
            is GenerationAction.Open ->
                uiState.value.jobs.find { it.id == action.id }?.storyId?.let {
                    sendEffect(
                        GenerationEffect.Read(it),
                    )
                }
            is GenerationAction.Refresh -> perform { refresh(action.id) is RefreshGeneration.Result.Updated }
            is GenerationAction.Retry -> perform { retry(action.id) is RetryGeneration.Result.Accepted }
        }
    }

    private fun perform(operation: suspend () -> Boolean) {
        updateState { copy(busy = true, error = null) }
        viewModelScope.launch {
            val success = operation()
            updateState { copy(busy = false, error = if (success) null else UiText.Resource(R.string.generation_unavailable)) }
        }
    }
}
