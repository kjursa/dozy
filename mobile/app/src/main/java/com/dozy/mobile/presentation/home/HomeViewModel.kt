package com.dozy.mobile.presentation.home

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.domain.character.ObserveCharacters
import com.kjursa.composeuikit.mvi.MviViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class HomeViewModel(
    observeCharacters: ObserveCharacters,
) : MviViewModel<HomeUiState, HomeAction, HomeEffect>(HomeUiState()) {
    init {
        viewModelScope.launch {
            observeCharacters().catch { updateState { copy(characterLoadError = true) } }.collect { collection ->
                updateState {
                    copy(
                        characters =
                            collection.characters.map { it.toHomeCharacter() },
                        characterLoadError = false,
                    )
                }
            }
        }
    }

    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CreateStoryClicked -> sendEffect(HomeEffect.CreateStoryRequested)
            is HomeAction.StorySelected -> sendEffect(HomeEffect.OpenStory(action.story))
            HomeAction.SeeAllStoriesClicked -> sendEffect(HomeEffect.SeeAllStoriesRequested)
            is HomeAction.CharacterSelected -> sendEffect(HomeEffect.OpenCharacter(action.character))
            HomeAction.AddCharacterClicked -> sendEffect(HomeEffect.AddCharacterRequested)
            HomeAction.NotificationsClicked -> sendEffect(HomeEffect.OpenNotifications)
        }
    }
}
