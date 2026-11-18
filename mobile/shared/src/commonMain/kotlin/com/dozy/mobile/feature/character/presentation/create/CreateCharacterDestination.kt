package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.navigation.NavigationCommand

sealed interface CreateCharacterDestination : NavigationCommand {
    data object NavigateBack : CreateCharacterDestination

    data class Saved(
        val id: Character.Id,
    ) : CreateCharacterDestination
}
