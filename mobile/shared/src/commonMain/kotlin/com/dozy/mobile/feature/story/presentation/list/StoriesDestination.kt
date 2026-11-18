package com.dozy.mobile.feature.story.presentation.list

import com.dozy.mobile.navigation.NavigationCommand

sealed interface StoriesDestination : NavigationCommand {
    data object CreateStory : StoriesDestination

    data class OpenStory(
        val story: Story,
    ) : StoriesDestination
}
