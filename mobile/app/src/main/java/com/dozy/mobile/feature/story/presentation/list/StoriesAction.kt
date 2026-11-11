package com.dozy.mobile.feature.story.presentation.list

internal sealed interface StoriesAction {
    data object Refresh : StoriesAction

    data object Create : StoriesAction

    data class StorySelected(
        val story: Story,
    ) : StoriesAction
}
