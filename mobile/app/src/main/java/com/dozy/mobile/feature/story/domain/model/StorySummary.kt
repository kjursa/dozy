package com.dozy.mobile.feature.story.domain.model

/** Metadata only; reading a summary must not require downloading the story text. */
data class StorySummary(
    val id: Story.Id,
    val title: String,
    val coverUrl: String,
    val readingMinutes: ReadingMinutes,
    val createdAt: CreatedAt,
) {
    @JvmInline value class ReadingMinutes(
        val value: Int,
    )

    @JvmInline value class CreatedAt(
        val epochMillis: Long,
    )
}
