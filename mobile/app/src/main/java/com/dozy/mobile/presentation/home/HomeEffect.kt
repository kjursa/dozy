package com.dozy.mobile.presentation.home

import com.dozy.mobile.presentation.stories.Story

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
