package com.dozy.mobile.feature.story.presentation.reader

import com.dozy.mobile.feature.story.domain.model.Story
import kotlin.math.ceil

/** Approximation for reading aloud at 150 words per minute. */
internal fun Story.readingMinutes(): Int = ceil(text.split(Regex("\\s+")).count { it.isNotBlank() } / 150.0).toInt().coerceAtLeast(1)
