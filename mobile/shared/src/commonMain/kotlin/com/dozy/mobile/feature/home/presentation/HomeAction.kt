package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.generation.GenerationAction
import com.dozy.mobile.feature.story.presentation.list.Story

sealed interface HomeAction {
    data object CreateStoryClicked : HomeAction

    data class StorySelected(
        val story: Story,
    ) : HomeAction

    data object SeeAllStoriesClicked : HomeAction

    data class CharacterSelected(
        val character: HomeCharacter,
    ) : HomeAction

    data object AddCharacterClicked : HomeAction

    data object SeeAllCharactersClicked : HomeAction

    data object SeeAllPlacesClicked : HomeAction

    data class PlaceSelected(
        val id: Place.Id,
    ) : HomeAction

    data class ProgressClicked(
        val id: String,
    ) : HomeAction

    data class Generation(
        val action: GenerationAction,
    ) : HomeAction

    data class RetrySection(
        val section: HomeSection,
    ) : HomeAction
}
