package com.dozy.mobile.feature.story.data

import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.place.data.toDomain
import com.dozy.mobile.feature.place.data.toDto
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.story.data.api.GenerationRequestDto
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.domain.model.*

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
        GenerationJob(GenerationJob.Id(it), GenerationJob.Status.valueOf(requireNotNull(status)))
    }

fun GenerationRequestDto.toDomain() =
    GenerationConfiguration(
        cast.map { it.toDomain() },
        GenerationJob.Mode.valueOf(mode),
        com.dozy.mobile.feature.story.domain.model.Story.Length
            .valueOf(length),
        place?.toEntity()?.toDomain(),
        idea,
        ending,
    )
