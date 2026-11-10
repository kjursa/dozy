package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.R
import com.dozy.mobile.feature.story.presentation.list.Story
import com.kjursa.foundation.presentation.text.UiText

internal data class HomeUiState(
    val dateLabel: UiText = UiText.Resource(R.string.tuesday_14_october),
    val greeting: UiText = UiText.Resource(R.string.good_evening),
    val prompt: UiText = UiText.Resource(R.string.ready_for_a_new_adventure),
    val stories: List<Story> =
        listOf(
            Story(UiText.Resource(R.string.oliver_and_the_secret_pirate_island), 6, R.drawable.story_pirates),
            Story(UiText.Resource(R.string.the_mystery_of_the_missing_moon), 10, R.drawable.story_moon),
            Story(UiText.Resource(R.string.a_journey_through_the_enchanted_forest), 8, R.drawable.story_forest),
        ),
    val characters: List<HomeCharacter> = emptyList(),
    val characterLoadError: Boolean = false,
)
