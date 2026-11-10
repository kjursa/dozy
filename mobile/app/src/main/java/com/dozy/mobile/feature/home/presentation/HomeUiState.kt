package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.R
import com.dozy.mobile.feature.story.presentation.list.Story

internal data class HomeUiState(
    val dateLabel: String = "TUESDAY, 14 OCTOBER",
    val greeting: String = "Good evening!",
    val prompt: String = "Ready for a new adventure?",
    val stories: List<Story> =
        listOf(
            Story("Oliver and the Secret Pirate Island", "6 min read", R.drawable.story_pirates),
            Story("The Mystery of the Missing Moon", "10 min read", R.drawable.story_moon),
            Story("A Journey Through the Enchanted Forest", "8 min read", R.drawable.story_forest),
        ),
    val characters: List<HomeCharacter> = emptyList(),
    val characterLoadError: Boolean = false,
)
