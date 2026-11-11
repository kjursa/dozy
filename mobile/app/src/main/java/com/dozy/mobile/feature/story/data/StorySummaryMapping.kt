package com.dozy.mobile.feature.story.data

import com.dozy.mobile.feature.story.data.api.StoryDto
import com.dozy.mobile.feature.story.data.api.StorySummaryDto
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.model.StorySummary
import kotlin.math.ceil

fun StorySummaryDto.toDomain(): StorySummary {
    require(id.matches(Regex("[A-Za-z0-9_-]{1,128}")))
    require(title.isNotBlank() && coverUrl.startsWith("https://") && coverUrl.length > 8)
    require(readingMinutes > 0 && createdAt >= 0)
    return StorySummary(Story.Id(id), title, coverUrl, StorySummary.ReadingMinutes(readingMinutes), StorySummary.CreatedAt(createdAt))
}

fun StoryDto.toSummaryDto() =
    StorySummaryDto(
        id,
        title,
        coverUrl,
        ceil(text.split(Regex("\\s+")).count { it.isNotBlank() } / 150.0).toInt().coerceAtLeast(1),
        createdAt,
    )
