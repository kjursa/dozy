package com.dozy.features.story.domain.model

data class StorySummary(val id: String, val title: String, val coverUrl: String, val readingMinutes: Int, val createdAt: Long)
data class StoryPage(val stories: List<StorySummary>, val nextCursor: String?)

fun readingMinutes(text: String): Int = maxOf(1, (text.split(Regex("\\s+")).count { it.isNotBlank() } + 149) / 150)
