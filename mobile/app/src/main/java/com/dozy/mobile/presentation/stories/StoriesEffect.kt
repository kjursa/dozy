package com.dozy.mobile.presentation.stories

internal sealed interface StoriesEffect {
    data class OpenStory(
        val story: Story,
    ) : StoriesEffect
}
