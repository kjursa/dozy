package com.dozy.mobile

import com.kjursa.composeuikit.mvi.MviViewModel

internal data class CharactersUiState(
    val eyebrow: String = "YOUR CAST",
    val title: String = "Characters",
    val description: String = "Meet the heroes of your bedtime stories.",
    val characters: List<Character> = sampleCharacters,
)

internal sealed interface CharactersAction {
    data class CharacterSelected(val character: Character) : CharactersAction
    data object AddCharacterClicked : CharactersAction
}

internal sealed interface CharactersEffect {
    data class NavigateToCharacterDetail(val character: Character) : CharactersEffect
    data object NavigateToAddCharacter : CharactersEffect
}

internal class CharactersViewModel : MviViewModel<CharactersUiState, CharactersAction, CharactersEffect>(CharactersUiState()) {
    override fun onAction(action: CharactersAction) {
        when (action) {
            is CharactersAction.CharacterSelected -> sendEffect(CharactersEffect.NavigateToCharacterDetail(action.character))
            CharactersAction.AddCharacterClicked -> sendEffect(CharactersEffect.NavigateToAddCharacter)
        }
    }
}
