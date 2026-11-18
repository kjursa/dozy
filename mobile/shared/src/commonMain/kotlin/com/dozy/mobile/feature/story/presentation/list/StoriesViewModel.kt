package com.dozy.mobile.feature.story.presentation.list

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class StoriesViewModel(
    val navigator: AppNavigator,
    private val observe: ObserveStoryLibrary,
    private val refresh: RefreshStories,
) : MviViewModel<StoriesUiState, StoriesAction, Nothing>(StoriesUiState()) {
    private var observation: Job? = null
    private var refreshing: Job? = null

    init {
        load()
    }

    private fun load() {
        if (refreshing?.isActive == true) return
        if (observation?.isActive != true) {
            updateState { copy(loading = true, error = null) }
            observation =
                viewModelScope.launch {
                    observe()
                        .catch {
                            updateState { copy(loading = false, error = UiText.Resource(Res.string.stories_load_error)) }
                        }.collect { stories ->
                            updateState { copy(loading = false, stories = stories.map { it.toUiStory() }) }
                        }
                }
        }
        updateState { copy(refreshing = true, error = null, offline = false) }
        refreshing =
            viewModelScope.launch {
                val result = refresh()
                updateState {
                    copy(
                        refreshing = false,
                        offline = result == RefreshStories.Result.Offline,
                        error = if (result == RefreshStories.Result.Failure) UiText.Resource(Res.string.stories_load_error) else error,
                    )
                }
            }
    }

    override fun onAction(action: StoriesAction) {
        if (uiState.value.navigating) return
        when (action) {
            StoriesAction.Refresh -> load()
            StoriesAction.Create -> navigate(StoriesDestination.CreateStory)
            is StoriesAction.StorySelected ->
                if (uiState.value.stories.any { it.id == action.story.id }) {
                    navigate(StoriesDestination.OpenStory(action.story))
                }
        }
    }

    private fun navigate(effect: StoriesDestination) {
        updateState { copy(navigating = true) }
        navigateTo(effect)
    }

    fun navigationHandled() {
        updateState { copy(navigating = false) }
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
