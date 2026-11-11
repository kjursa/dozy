package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.generation.GenerationUiState
import com.dozy.mobile.feature.story.presentation.list.Story
import com.kjursa.foundation.presentation.text.UiText

internal enum class HomeSection { STORIES, CHARACTERS, PLACES, GENERATION }

internal data class HomeUiState(
    val stories: List<Story> = emptyList(),
    val characters: List<HomeCharacter> = emptyList(),
    val places: List<Place> = emptyList(),
    val generation: GenerationUiState = GenerationUiState(),
    val loading: Set<HomeSection> = HomeSection.entries.toSet(),
    val refreshing: Set<HomeSection> = emptySet(),
    val errors: Map<HomeSection, UiText> = emptyMap(),
    val navigating: Boolean = false,
) {
    val firstVisit: Boolean
        get() =
            characters.isEmpty() &&
                stories.isEmpty() &&
                HomeSection.CHARACTERS !in loading &&
                HomeSection.STORIES !in loading &&
                HomeSection.CHARACTERS !in refreshing &&
                HomeSection.STORIES !in refreshing &&
                HomeSection.CHARACTERS !in errors &&
                HomeSection.STORIES !in errors
}
