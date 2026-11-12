package com.dozy.mobile.feature.character.presentation.list

import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText

data class CharactersUiState(
    val eyebrow: UiText = UiText.Resource(Res.string.your_cast),
    val title: UiText = UiText.Resource(Res.string.characters),
    val subtitle: UiText = UiText.Resource(Res.string.meet_the_heroes_of_your_bedtime_stories),
    val characters: List<CharacterListItem> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val offline: Boolean = false,
    val characterLoadError: Boolean = false,
)
