package com.dozy.mobile.feature.story.presentation.reader

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.usecase.ReadStory
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import kotlinx.coroutines.launch

class ReaderViewModel(
    val navigator: AppNavigator,
    private val id: Story.Id,
    private val readStory: ReadStory,
    persistence: StatePersistence<ReaderUiState>,
) : MviViewModel<ReaderUiState, ReaderAction, Nothing>(ReaderUiState(), persistence) {
    private var navigating = false

    init {
        load()
    }

    fun resumed() {
        navigating = false
    }

    private fun load() {
        updateState { copy(loading = true, unavailable = false) }
        viewModelScope.launch {
            val result = readStory(id)
            updateState {
                copy(
                    loading = false,
                    story = (result as? ReadStory.Result.Found)?.story,
                    unavailable = result == ReadStory.Result.Unavailable,
                )
            }
        }
    }

    override fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.Retry -> if (!uiState.value.loading) load()
            ReaderAction.Settings -> updateState { copy(overlay = ReaderUiState.Overlay.SETTINGS) }
            ReaderAction.Cover -> if (uiState.value.story != null) updateState { copy(overlay = ReaderUiState.Overlay.COVER) }
            ReaderAction.Dismiss -> updateState { copy(overlay = null) }
            is ReaderAction.Size -> updateState { copy(textSize = action.size) }
            is ReaderAction.Evening -> updateState { copy(evening = action.enabled) }
            ReaderAction.Back ->
                if (uiState.value.overlay !=
                    null
                ) {
                    updateState { copy(overlay = null) }
                } else {
                    navigate(ReaderDestination.Back)
                }
            ReaderAction.Library -> navigate(ReaderDestination.Library)
            ReaderAction.NewAdventure ->
                uiState.value.story?.let { story ->
                    navigate(ReaderDestination.NewAdventure(story.configuration.cast.map { it.id }))
                }
        }
    }

    private fun navigate(effect: ReaderDestination) {
        if (navigating) return
        navigating = true
        navigateTo(effect)
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
