package com.dozy.mobile.feature.story.data

import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.place.data.toDomain
import com.dozy.mobile.feature.place.data.toDto
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.data.database.GenerationProgress
import com.dozy.mobile.feature.story.data.database.StoryEntity
import com.dozy.mobile.feature.story.domain.model.GenerationConfiguration
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story

fun GenerationConfiguration.toDto() =
    GenerationRequestDto(
        cast.map { it.toDto() },
        mode.name,
        length.name,
        if (mode == GenerationJob.Mode.MANUAL) place?.toDto() else null,
        if (mode == GenerationJob.Mode.MANUAL) idea else null,
        if (mode == GenerationJob.Mode.MANUAL) ending else null,
    )

fun GenerationEntity.toJob(): GenerationJob? =
    GenerationProgress(jobId, status, null, errorCode, statusUnknown, false, retryKey, createdOrder).toJob()

fun GenerationProgress.toJob(): GenerationJob? =
    jobId?.let {
        val stage = GenerationJob.Status.valueOf(requireNotNull(status))
        GenerationJob(
            GenerationJob.Id(it),
            if (stage == GenerationJob.Status.READY && storyId == null) GenerationJob.Status.CREATING_COVER else stage,
            storyId?.let { id -> Story.Id(id) },
            errorCode?.let(GenerationJob.Failure::valueOf),
            statusUnknown,
            notificationDelivered,
            retryKey != null,
        )
    }

fun StoryEntity.toCompletedJob(): GenerationJob? =
    if (requestKey != null && result != null) {
        GenerationProgress(jobId, "READY", id, null, false, notificationDelivered, null, createdOrder).toJob()
    } else {
        null
    }

fun GenerationRequestDto.toDomain(allowLocalAssetHttp: Boolean = false) =
    GenerationConfiguration(
        cast.map { it.toDomain(allowLocalAssetHttp) },
        GenerationJob.Mode.valueOf(mode),
        Story.Length.valueOf(length),
        place?.toEntity()?.toDomain(),
        idea,
        ending,
    )

fun StoryDto.toDomain(allowLocalAssetHttp: Boolean = false): Story {
    require(id.matches(Regex("[A-Za-z0-9_-]{1,128}")))
    require(title.isNotBlank() && text.isNotBlank() && validStoryCoverUrl(coverUrl))
    require(configuration.cast.isNotEmpty() && configuration.cast.distinctBy { it.id }.size == configuration.cast.size)
    return Story(Story.Id(id), title, text, coverUrl, configuration.toDomain(allowLocalAssetHttp))
}
