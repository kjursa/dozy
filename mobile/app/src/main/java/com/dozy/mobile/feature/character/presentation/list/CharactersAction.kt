package com.dozy.mobile.feature.character.presentation.list

internal sealed interface CharactersAction {
    data class CharacterSelected(
        val character: CharacterListItem,
    ) : CharactersAction

    data object Refresh : CharactersAction

    data object AddCharacterClicked : CharactersAction
}
