package com.dozy.mobile.feature.character.presentation.list

import com.dozy.mobile.R
import com.kjursa.foundation.presentation.text.UiText

internal data class CharactersUiState(
    val eyebrow: UiText = UiText.Resource(R.string.your_cast),
    val title: UiText = UiText.Resource(R.string.characters),
    val subtitle: UiText = UiText.Resource(R.string.meet_the_heroes_of_your_bedtime_stories),
    val characters: List<CharacterListItem> = emptyList(),
    val characterLoadError: Boolean = false,
)
