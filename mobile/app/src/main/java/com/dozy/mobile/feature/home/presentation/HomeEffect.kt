package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.story.presentation.list.Story

internal sealed interface HomeEffect {
    data object CreateStoryRequested : HomeEffect

    data class OpenStory(
        val story: Story,
    ) : HomeEffect

    data object SeeAllStoriesRequested : HomeEffect

    data class OpenCharacter(
        val character: HomeCharacter,
    ) : HomeEffect

    data object AddCharacterRequested : HomeEffect

    data object OpenNotifications : HomeEffect
}
