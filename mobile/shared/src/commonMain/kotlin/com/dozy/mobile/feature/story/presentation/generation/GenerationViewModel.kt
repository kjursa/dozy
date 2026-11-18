package com.dozy.mobile.feature.story.presentation.generation

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.story.domain.usecase.*
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GenerationViewModel(
    val navigator: AppNavigator,
    observe: ObserveGenerations,
    private val refresh: RefreshGeneration,
    private val retry: RetryGeneration,
) : MviViewModel<GenerationUiState, GenerationAction, Nothing>(GenerationUiState()) {
    init {
        viewModelScope.launch {
            observe()
                .catch { updateState { copy(loading = false, error = UiText.Resource(Res.string.generation_unavailable)) } }
                .collect { updateState { copy(loading = false, jobs = it) } }
        }
    }

    override fun onAction(action: GenerationAction) {
        if (uiState.value.busy) return
        when (action) {
            is GenerationAction.Open ->
                uiState.value.jobs.find { it.id == action.id }?.storyId?.let {
                    navigateTo(
                        GenerationDestination.Read(it),
                    )
                }
            is GenerationAction.Refresh -> perform { refresh(action.id) is RefreshGeneration.Result.Updated }
            is GenerationAction.Retry -> {
                val job = uiState.value.jobs.find { it.id == action.id } ?: return
                if (job.statusUnknown || job.retryPending) return
                perform { retry(action.id) is RetryGeneration.Result.Accepted }
            }
        }
    }

    private fun perform(operation: suspend () -> Boolean) {
        updateState { copy(busy = true, error = null) }
        viewModelScope.launch {
            val success = operation()
            updateState { copy(busy = false, error = if (success) null else UiText.Resource(Res.string.generation_unavailable)) }
        }
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
