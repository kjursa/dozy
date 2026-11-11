package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.R
import com.dozy.mobile.feature.story.presentation.list.Story
import com.kjursa.foundation.presentation.text.UiText

internal data class HomeUiState(
    val dateLabel: UiText = UiText.Resource(R.string.tuesday_14_october),
    val greeting: UiText = UiText.Resource(R.string.good_evening),
    val prompt: UiText = UiText.Resource(R.string.ready_for_a_new_adventure),
    val stories: List<Story> = emptyList(),
    val characters: List<HomeCharacter> = emptyList(),
    val characterLoadError: Boolean = false,
)
