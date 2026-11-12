package com.dozy.mobile.feature.story.domain.model

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.place.domain.model.Place
import kotlin.jvm.JvmInline

/** Immutable values captured when a request is first submitted. */
data class GenerationConfiguration(
    val cast: List<Character>,
    val mode: GenerationJob.Mode,
    val length: Story.Length,
    val place: Place?,
    val idea: String?,
    val ending: String?,
)

data class GenerationJob(
    val id: Id,
    val status: Status,
    val storyId: Story.Id? = null,
    val failure: Failure? = null,
    val statusUnknown: Boolean = false,
    val notificationDelivered: Boolean = false,
    val retryPending: Boolean = false,
) {
    @JvmInline value class Id(
        val value: String,
    )

    @JvmInline value class RequestKey(
        val value: String,
    )

    enum class Failure { CONTENT_REJECTED, GENERATION_FAILED }

    enum class Mode { MANUAL, AUTOMATIC }

    enum class Status { QUEUED, CREATING_TEXT, CREATING_COVER, READY, FAILED }
}

data class Story(
    val id: Id,
    val title: String,
    val text: String,
    val coverUrl: String,
    val configuration: GenerationConfiguration,
) {
    @JvmInline value class Id(
        val value: String,
    )

    enum class Length { SHORT, LONG }
}
