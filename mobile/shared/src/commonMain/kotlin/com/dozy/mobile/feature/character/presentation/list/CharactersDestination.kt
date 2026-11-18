package com.dozy.mobile.feature.character.presentation.list

import com.dozy.mobile.navigation.NavigationCommand

sealed interface CharactersDestination : NavigationCommand {
    data class OpenCharacter(
        val character: CharacterListItem,
    ) : CharactersDestination

    data object NavigateToAddCharacter : CharactersDestination
}
