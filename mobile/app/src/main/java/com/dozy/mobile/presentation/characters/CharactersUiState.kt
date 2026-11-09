package com.dozy.mobile.presentation.characters

internal data class CharactersUiState(
    val eyebrow: String = "YOUR CAST",
    val title: String = "Characters",
    val subtitle: String = "Meet the heroes of your bedtime stories.",
    val characters: List<CharacterListItem> = emptyList(),
    val characterLoadError: Boolean = false,
)
