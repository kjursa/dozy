package com.dozy.mobile

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.domain.ObserveCharacters
import com.kjursa.composeuikit.mvi.MviViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal data class Character(
    val name: String,
    val image: Int,
)

internal data class CharactersUiState(
    val eyebrow: String = "YOUR CAST",
    val title: String = "Characters",
    val subtitle: String = "Meet the heroes of your bedtime stories.",
    val characters: List<Character> = emptyList(),
    val characterLoadError: Boolean = false,
)

internal sealed interface CharactersAction {
    data class CharacterSelected(
        val character: Character,
    ) : CharactersAction

    data object AddCharacterClicked : CharactersAction
}

internal sealed interface CharactersEffect {
    data class OpenCharacter(
        val character: Character,
    ) : CharactersEffect

    data object NavigateToAddCharacter : CharactersEffect
}

internal class CharactersViewModel(
    observeCharacters: ObserveCharacters,
) : MviViewModel<CharactersUiState, CharactersAction, CharactersEffect>(CharactersUiState()) {
    init {
        viewModelScope.launch {
            observeCharacters().catch { updateState { copy(characterLoadError = true) } }.collect { collection ->
                updateState {
                    copy(
                        characters = collection.characters.map { Character(it.details.name.value, R.drawable.character_oliver) },
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
