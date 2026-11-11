package com.dozy.mobile.feature.story.presentation.list

import com.dozy.mobile.R
import com.kjursa.foundation.presentation.text.UiText

internal data class StoriesUiState(
    val eyebrow: UiText = UiText.Resource(R.string.your_library),
    val title: UiText = UiText.Resource(R.string.my_stories),
    val subtitle: UiText = UiText.Resource(R.string.pick_up_where_your_last_adventure_left_off),
    val collectionEyebrow: UiText = UiText.Resource(R.string.library),
    val collectionTitle: UiText = UiText.Resource(R.string.your_stories),
    val stories: List<Story> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiText? = null,
    val offline: Boolean = false,
    val navigating: Boolean = false,
)
