package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.list.Story
import com.dozy.mobile.navigation.NavigationCommand

sealed interface HomeDestination : NavigationCommand {
    data object CreateStoryRequested : HomeDestination

    data class OpenStory(
        val story: Story,
    ) : HomeDestination

    data object SeeAllStoriesRequested : HomeDestination

    data class OpenCharacter(
        val character: HomeCharacter,
    ) : HomeDestination

    data object AddCharacterRequested : HomeDestination

    data object SeeAllCharactersRequested : HomeDestination

    data object SeeAllPlacesRequested : HomeDestination

    data class CreateStoryAtPlace(
        val id: Place.Id,
    ) : HomeDestination

    data class OpenProgress(
        val id: String,
    ) : HomeDestination

    data class ReadStory(
        val id: String,
    ) : HomeDestination
}
