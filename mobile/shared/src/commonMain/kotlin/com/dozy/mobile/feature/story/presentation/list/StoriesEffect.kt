package com.dozy.mobile.feature.story.presentation.list

sealed interface StoriesEffect {
    data object CreateStory : StoriesEffect

    data class OpenStory(
        val story: Story,
    ) : StoriesEffect
}
