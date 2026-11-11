package com.dozy.mobile

import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.data.database.*
import com.dozy.mobile.feature.story.domain.usecase.StartGeneration
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeGenerationDao : GenerationDao {
    val rows = MutableStateFlow<List<GenerationEntity>>(emptyList())

    override suspend fun find(key: String) = rows.value.find { it.requestKey == key }

    override fun observe() = rows

    override suspend fun upsert(entity: GenerationEntity) {
        rows.value = rows.value.filterNot { it.requestKey == entity.requestKey } + entity
    }
}

internal fun testStartGeneration() =
    StartGeneration(
        DefaultGenerationRepository(
            FakeGenerationDao(),
            object : GenerationApi {
                override suspend fun start(
                    key: String,
                    request: GenerationRequestDto,
                ): GenerationAcceptedDto? = null
            },
        ),
        FakeCharacterRepository(),
        FakePlaceRepository(),
    )
