package com.dozy.mobile.feature.character.presentation.list

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.RefreshCharacters
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.kjursa.foundation.compose.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CharactersViewModel(
    val navigator: AppNavigator,
    private val observeCharacters: ObserveCharacters,
    private val refreshCharacters: RefreshCharacters,
) : MviViewModel<CharactersUiState, CharactersAction, Nothing>(CharactersUiState()) {
    private var observation: Job? = null
    private var observationFailed = false
    private var refreshFailed = false

    init {
        observe()
        refresh()
    }

    private fun observe() {
        observation?.cancel()
        observation =
            viewModelScope.launch {
                observeCharacters()
                    .catch {
                        observationFailed = true
                        updateState { copy(characterLoadError = true, loading = false) }
                    }.collect { collection ->
                        observationFailed = false
                        updateState {
                            copy(
                                characters = collection.characters.map { it.toCharacterListItem() },
                                loading = false,
                                characterLoadError = refreshFailed,
                            )
                        }
                    }
            }
    }

    private fun refresh() {
        if (uiState.value.refreshing) return
        refreshFailed = false
        updateState { copy(refreshing = true, characterLoadError = false, offline = false) }
        viewModelScope.launch {
            val result = refreshCharacters()
            refreshFailed = result == RefreshCharacters.Result.Failure
            updateState {
                copy(
                    refreshing = false,
                    offline = result == RefreshCharacters.Result.Offline,
                    characterLoadError =
                        observationFailed || refreshFailed,
                )
            }
        }
    }

    override fun onAction(action: CharactersAction) {
        when (action) {
            CharactersAction.Refresh -> {
                if (observationFailed) observe()
                refresh()
            }
            is CharactersAction.CharacterSelected -> navigateTo(CharactersDestination.OpenCharacter(action.character))
            CharactersAction.AddCharacterClicked -> navigateTo(CharactersDestination.NavigateToAddCharacter)
        }
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
