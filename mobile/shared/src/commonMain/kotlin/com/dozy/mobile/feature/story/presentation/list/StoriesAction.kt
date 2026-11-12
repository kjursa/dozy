package com.dozy.mobile.feature.story.presentation.list

sealed interface StoriesAction {
    data object Refresh : StoriesAction

    data object Create : StoriesAction

    data class StorySelected(
        val story: Story,
    ) : StoriesAction
}
