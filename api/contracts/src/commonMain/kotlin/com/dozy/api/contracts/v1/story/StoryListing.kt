package com.dozy.api.contracts.v1.story

import com.dozy.api.contracts.v1.ValidationReason
import kotlinx.serialization.Serializable

@Serializable
data class ListStoriesRequest(val limit: Int = 100, val cursor: String? = null)

@Serializable
data class StorySummaryDto(val id: String, val title: String, val coverUrl: String, val readingMinutes: Int, val createdAt: Long)

@Serializable
data class StoryPageDto(val stories: List<StorySummaryDto>, val nextCursor: String? = null)

@Serializable
data class StorySyncDto(
    val upserts: List<StorySummaryDto>,
    val deletedIds: List<String>,
    val nextRevision: Long,
)

enum class StoryListField(val value: String, val reasons: Set<ValidationReason>) {
    LIMIT("limit", setOf(ValidationReason.INVALID_VALUE));
}
