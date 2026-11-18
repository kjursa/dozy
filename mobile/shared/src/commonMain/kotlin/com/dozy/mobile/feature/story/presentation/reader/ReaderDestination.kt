package com.dozy.mobile.feature.story.presentation.reader

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.navigation.NavigationCommand

sealed interface ReaderDestination : NavigationCommand {
    data object Back : ReaderDestination

    data object Library : ReaderDestination

    data class NewAdventure(
        val cast: List<Character.Id>,
    ) : ReaderDestination
}
