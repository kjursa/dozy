package com.dozy.mobile.presentation.characters

internal sealed interface CharactersAction {
    data class CharacterSelected(
        val character: CharacterListItem,
    ) : CharactersAction

    data object AddCharacterClicked : CharactersAction
}
