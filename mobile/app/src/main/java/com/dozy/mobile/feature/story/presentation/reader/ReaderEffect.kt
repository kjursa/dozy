package com.dozy.mobile.feature.story.presentation.reader

import com.dozy.mobile.feature.character.domain.model.Character

internal sealed interface ReaderEffect {
    data object Back : ReaderEffect

    data object Library : ReaderEffect

    data class NewAdventure(
        val cast: List<Character.Id>,
    ) : ReaderEffect
}
