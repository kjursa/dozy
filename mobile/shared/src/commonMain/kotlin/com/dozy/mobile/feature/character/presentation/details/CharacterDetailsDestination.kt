package com.dozy.mobile.feature.character.presentation.details

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.navigation.NavigationCommand

sealed interface CharacterDetailsDestination : NavigationCommand {
    data object Back : CharacterDetailsDestination

    data object Deleted : CharacterDetailsDestination

    data class Edit(
        val id: Character.Id,
    ) : CharacterDetailsDestination

    data class Story(
        val id: Character.Id,
    ) : CharacterDetailsDestination
}
