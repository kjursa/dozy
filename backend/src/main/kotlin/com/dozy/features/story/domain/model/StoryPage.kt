package com.dozy.features.story.domain.model

import com.dozy.domain.id.LibraryCursor
import com.dozy.domain.id.StoryId

data class StorySummary(val id: StoryId, val title: String, val coverUrl: String, val readingMinutes: Int, val createdAt: Long)
data class StoryPage(val stories: List<StorySummary>, val nextCursor: LibraryCursor?)

fun readingMinutes(text: String): Int = maxOf(1, (text.split(Regex("\\s+")).count { it.isNotBlank() } + 149) / 150)
