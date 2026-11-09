package com.dozy.mobile.presentation.stories

internal sealed interface StoriesAction {
    data class StorySelected(
        val story: Story,
    ) : StoriesAction
}
