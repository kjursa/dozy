package com.dozy.mobile.presentation.stories

import com.kjursa.composeuikit.mvi.MviViewModel

internal class StoriesViewModel : MviViewModel<StoriesUiState, StoriesAction, StoriesEffect>(StoriesUiState()) {
    override fun onAction(action: StoriesAction) {
        when (action) {
            is StoriesAction.StorySelected -> sendEffect(StoriesEffect.OpenStory(action.story))
        }
    }
}
