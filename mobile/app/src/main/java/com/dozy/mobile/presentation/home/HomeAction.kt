package com.dozy.mobile.presentation.home

import com.dozy.mobile.presentation.stories.Story

internal sealed interface HomeAction {
    data object CreateStoryClicked : HomeAction

    data class StorySelected(
        val story: Story,
    ) : HomeAction

    data object SeeAllStoriesClicked : HomeAction

    data class CharacterSelected(
        val character: HomeCharacter,
    ) : HomeAction

    data object AddCharacterClicked : HomeAction

    data object NotificationsClicked : HomeAction
}
