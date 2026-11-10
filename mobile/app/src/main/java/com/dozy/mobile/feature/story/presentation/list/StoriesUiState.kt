package com.dozy.mobile.feature.story.presentation.list

import com.dozy.mobile.R
import com.kjursa.foundation.presentation.text.UiText

internal data class StoriesUiState(
    val eyebrow: UiText = UiText.Resource(R.string.your_library),
    val title: UiText = UiText.Resource(R.string.my_stories),
    val subtitle: UiText = UiText.Resource(R.string.pick_up_where_your_last_adventure_left_off),
    val collectionEyebrow: UiText = UiText.Resource(R.string.library),
    val collectionTitle: UiText = UiText.Resource(R.string.your_stories),
    val stories: List<Story> =
        listOf(
            Story(UiText.Resource(R.string.oliver_and_the_secret_pirate_island), 6, R.drawable.story_pirates),
            Story(UiText.Resource(R.string.the_mystery_of_the_missing_moon), 10, R.drawable.story_moon),
            Story(UiText.Resource(R.string.a_journey_through_the_enchanted_forest), 8, R.drawable.story_forest),
        ),
)
