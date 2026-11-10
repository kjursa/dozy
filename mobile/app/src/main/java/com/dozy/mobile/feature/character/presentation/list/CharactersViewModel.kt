package com.dozy.mobile.feature.character.presentation.list

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.ObserveCharacters
import com.kjursa.composeuikit.mvi.MviViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class CharactersViewModel(
    observeCharacters: ObserveCharacters,
) : MviViewModel<CharactersUiState, CharactersAction, CharactersEffect>(CharactersUiState()) {
    init {
        viewModelScope.launch {
            observeCharacters().catch { updateState { copy(characterLoadError = true) } }.collect { collection ->
                updateState {
                    copy(
                        characters = collection.characters.map { it.toCharacterListItem() },
                        characterLoadError = false,
                    )
                }
            }
        }
    }

    override fun onAction(action: CharactersAction) {
        when (action) {
            is CharactersAction.CharacterSelected -> sendEffect(CharactersEffect.OpenCharacter(action.character))
            CharactersAction.AddCharacterClicked -> sendEffect(CharactersEffect.NavigateToAddCharacter)
        }
    }
}
