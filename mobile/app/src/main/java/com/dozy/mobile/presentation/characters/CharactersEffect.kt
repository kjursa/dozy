package com.dozy.mobile.presentation.characters

internal sealed interface CharactersEffect {
    data class OpenCharacter(
        val character: CharacterListItem,
    ) : CharactersEffect

    data object NavigateToAddCharacter : CharactersEffect
}
