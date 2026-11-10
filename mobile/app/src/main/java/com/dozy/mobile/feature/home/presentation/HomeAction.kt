package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.story.presentation.list.Story

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
