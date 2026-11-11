package com.dozy.mobile.feature.story.presentation.list

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.R
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class StoriesViewModel(
    private val observe: ObserveStoryLibrary,
    private val refresh: RefreshStories,
) : MviViewModel<StoriesUiState, StoriesAction, StoriesEffect>(StoriesUiState()) {
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
                            updateState { copy(loading = false, error = UiText.Resource(R.string.stories_load_error)) }
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
                        error = if (result == RefreshStories.Result.Failure) UiText.Resource(R.string.stories_load_error) else error,
                    )
                }
            }
    }

    override fun onAction(action: StoriesAction) {
        if (uiState.value.navigating) return
        when (action) {
            StoriesAction.Refresh -> load()
            StoriesAction.Create -> navigate(StoriesEffect.CreateStory)
            is StoriesAction.StorySelected ->
                if (uiState.value.stories.any { it.id == action.story.id }) {
                    navigate(StoriesEffect.OpenStory(action.story))
                }
        }
    }

    private fun navigate(effect: StoriesEffect) {
        updateState { copy(navigating = true) }
        sendEffect(effect)
    }

    fun navigationHandled() {
        updateState { copy(navigating = false) }
    }
}
