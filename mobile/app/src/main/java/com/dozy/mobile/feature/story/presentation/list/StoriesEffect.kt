package com.dozy.mobile.feature.story.presentation.list

internal sealed interface StoriesEffect {
    data class OpenStory(
        val story: Story,
    ) : StoriesEffect
}
