package com.dozy.mobile.presentation.stories

import com.dozy.mobile.R

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
