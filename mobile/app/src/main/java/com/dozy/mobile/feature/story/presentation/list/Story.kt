package com.dozy.mobile.feature.story.presentation.list

import com.dozy.mobile.feature.story.domain.model.StorySummary
import com.kjursa.foundation.presentation.text.UiText

internal data class Story(
    val title: UiText,
    val minutes: Int,
    val coverUrl: String,
    val id: String,
)

internal fun StorySummary.toUiStory() = Story(UiText.Plain(title), readingMinutes.value, coverUrl, id.value)
