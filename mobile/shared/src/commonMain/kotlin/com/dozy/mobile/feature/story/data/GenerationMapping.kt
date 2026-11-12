package com.dozy.mobile.feature.story.data

import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.place.data.toDomain
import com.dozy.mobile.feature.place.data.toDto
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.story.data.api.GenerationRequestDto
import com.dozy.mobile.feature.story.data.api.StoryDto
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.domain.model.*
import kotlinx.serialization.json.Json

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
    jobId?.let {
        GenerationJob(
            GenerationJob.Id(it),
            if (status == "READY" &&
                result == null
            ) {
                GenerationJob.Status.CREATING_COVER
            } else {
                GenerationJob.Status.valueOf(requireNotNull(status))
            },
            result?.let { json ->
                Json.decodeFromString<StoryDto>(json).toDomain().id
            },
            errorCode?.let(GenerationJob.Failure::valueOf),
            statusUnknown,
            notificationDelivered,
            retryKey != null,
        )
    }

fun GenerationRequestDto.toDomain() =
    GenerationConfiguration(
        cast.map { it.toDomain() },
        GenerationJob.Mode.valueOf(mode),
        Story.Length.valueOf(length),
        place?.toEntity()?.toDomain(),
        idea,
        ending,
    )

fun StoryDto.toDomain(): Story {
    require(id.matches(Regex("[A-Za-z0-9_-]{1,128}")))
    require(title.isNotBlank() && text.isNotBlank() && coverUrl.startsWith("https://"))
    require(configuration.cast.isNotEmpty() && configuration.cast.distinctBy { it.id }.size == configuration.cast.size)
    return Story(Story.Id(id), title, text, coverUrl, configuration.toDomain())
}
