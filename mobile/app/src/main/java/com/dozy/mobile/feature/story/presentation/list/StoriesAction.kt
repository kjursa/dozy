package com.dozy.mobile.feature.story.presentation.list

internal sealed interface StoriesAction {
    data class StorySelected(
        val story: Story,
    ) : StoriesAction
}
