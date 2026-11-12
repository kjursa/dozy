package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.list.Story

sealed interface HomeEffect {
    data object CreateStoryRequested : HomeEffect

    data class OpenStory(
        val story: Story,
    ) : HomeEffect

    data object SeeAllStoriesRequested : HomeEffect

    data class OpenCharacter(
        val character: HomeCharacter,
    ) : HomeEffect

    data object AddCharacterRequested : HomeEffect

    data object SeeAllCharactersRequested : HomeEffect

    data object SeeAllPlacesRequested : HomeEffect

    data class CreateStoryAtPlace(
        val id: Place.Id,
    ) : HomeEffect

    data class OpenProgress(
        val id: String,
    ) : HomeEffect

    data class ReadStory(
        val id: String,
    ) : HomeEffect
}
