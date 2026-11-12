package com.dozy.mobile.feature.character.presentation.list

sealed interface CharactersEffect {
    data class OpenCharacter(
        val character: CharacterListItem,
    ) : CharactersEffect

    data object NavigateToAddCharacter : CharactersEffect
}
