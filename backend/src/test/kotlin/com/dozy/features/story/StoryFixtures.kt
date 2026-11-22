package com.dozy.features.story

import com.dozy.config.GenerationConfig
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.PlaceId
import com.dozy.features.story.data.database.StoryChangesDao
import com.dozy.features.story.data.repository.SqliteGenerationRepository
import com.dozy.features.story.domain.model.HeroSnapshot
import com.dozy.features.story.domain.model.PlaceSnapshot
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.StoryConfiguration
import com.dozy.infrastructure.database.SqliteDatabase
import java.time.Clock
import java.util.UUID

internal fun configuration() = StoryConfiguration(
    listOf(HeroSnapshot(requireNotNull(CharacterId.parse("char-019a0000-0000-7000-8000-000000000001")), "Luna", "Kind", "Silver coat", "ANIMAL", null)),
    StoryConfiguration.Mode.MANUAL, StoryConfiguration.Length.SHORT,
    PlaceSnapshot(requireNotNull(PlaceId.parse("place-00000000-0000-7000-8000-000000000001")), "Moon garden", "A peaceful place", "SYSTEM", false, null), "  Find a star  ", "  Home  ",
)
internal fun command(config: StoryConfiguration = configuration()) = (StartGeneration.parse(config) as StartGeneration.Result.Valid).command

internal fun requestKey(): IdempotencyKey = requireNotNull(IdempotencyKey.parse(UUID.randomUUID().toString()))


internal fun testGenerationRepository(
    database: SqliteDatabase,
    config: GenerationConfig,
    clock: Clock,
) = SqliteGenerationRepository(database, config, clock, StoryChangesDao(database))
