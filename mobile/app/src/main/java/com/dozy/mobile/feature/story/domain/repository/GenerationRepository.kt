package com.dozy.mobile.feature.story.domain.repository

import com.dozy.mobile.feature.story.domain.model.GenerationConfiguration
import com.dozy.mobile.feature.story.domain.model.GenerationJob

interface GenerationRepository {
    suspend fun configuration(key: GenerationJob.RequestKey): GenerationConfiguration?

    suspend fun accepted(key: GenerationJob.RequestKey): GenerationJob?

    suspend fun start(
        key: GenerationJob.RequestKey,
        configuration: GenerationConfiguration,
    ): GenerationJob?
}
