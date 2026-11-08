package com.dozy.mobile

import com.kjursa.composeuikit.mvi.MviViewModel

internal data class Story(
    val title: String,
    val minutes: String,
    val image: Int,
)

internal data class StoriesUiState(
    val eyebrow: String = "YOUR LIBRARY",
    val title: String = "My Stories",
    val subtitle: String = "Pick up where your last adventure left off.",
    val collectionEyebrow: String = "LIBRARY",
    val collectionTitle: String = "Your Stories",
    val stories: List<Story> =
        listOf(
            Story("Oliver and the Secret Pirate Island", "6 min read", R.drawable.story_pirates),
            Story("The Mystery of the Missing Moon", "10 min read", R.drawable.story_moon),
            Story("A Journey Through the Enchanted Forest", "8 min read", R.drawable.story_forest),
        ),
)

internal sealed interface StoriesAction {
    data class StorySelected(
        val story: Story,
    ) : StoriesAction
}

internal sealed interface StoriesEffect {
    data class OpenStory(
        val story: Story,
    ) : StoriesEffect
}

internal class StoriesViewModel : MviViewModel<StoriesUiState, StoriesAction, StoriesEffect>(StoriesUiState()) {
    override fun onAction(action: StoriesAction) {
        when (action) {
            is StoriesAction.StorySelected -> sendEffect(StoriesEffect.OpenStory(action.story))
        }
    }
}
