package com.dozy.mobile.feature.story.presentation.list

import com.kjursa.foundation.compose.mvi.MviViewModel

internal class StoriesViewModel : MviViewModel<StoriesUiState, StoriesAction, StoriesEffect>(StoriesUiState()) {
    override fun onAction(action: StoriesAction) {
        when (action) {
            is StoriesAction.StorySelected -> sendEffect(StoriesEffect.OpenStory(action.story))
        }
    }
}
