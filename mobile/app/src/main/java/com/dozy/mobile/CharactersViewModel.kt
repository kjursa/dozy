package com.dozy.mobile

import com.kjursa.composeuikit.mvi.MviViewModel

internal data class Character(
    val name: String,
    val image: Int,
)

internal data class CharactersUiState(
    val eyebrow: String = "YOUR CAST",
    val title: String = "Characters",
    val subtitle: String = "Meet the heroes of your bedtime stories.",
    val characters: List<Character> =
        listOf(
            Character("Oliver", R.drawable.character_oliver),
            Character("Luna", R.drawable.character_luna),
            Character("Captain Finn", R.drawable.character_finn),
        ),
)

internal sealed interface CharactersAction {
    data class CharacterSelected(val character: Character) : CharactersAction
    data object AddCharacterClicked : CharactersAction
}

internal sealed interface CharactersEffect {
    data class OpenCharacter(val character: Character) : CharactersEffect
    data object NavigateToAddCharacter : CharactersEffect
}

internal class CharactersViewModel : MviViewModel<CharactersUiState, CharactersAction, CharactersEffect>(CharactersUiState()) {
    override fun onAction(action: CharactersAction) {
        when (action) {
            is CharactersAction.CharacterSelected -> sendEffect(CharactersEffect.OpenCharacter(action.character))
            CharactersAction.AddCharacterClicked -> sendEffect(CharactersEffect.NavigateToAddCharacter)
        }
    }
}
