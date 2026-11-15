package com.dozy.features.story

import com.dozy.features.story.domain.model.HeroSnapshot
import com.dozy.features.story.domain.model.PlaceSnapshot
import com.dozy.features.story.domain.model.StoryConfiguration
import com.dozy.features.story.domain.model.StartGeneration

internal fun configuration() = StoryConfiguration(
    listOf(HeroSnapshot("019a0000-0000-7000-8000-000000000001", "Luna", "Kind", "Silver coat", "ANIMAL", null)),
    StoryConfiguration.Mode.MANUAL, StoryConfiguration.Length.SHORT,
    PlaceSnapshot("bundled:moon", "Moon garden", "A peaceful place", "SYSTEM", false, null), "  Find a star  ", "  Home  ",
)
internal fun command(config: StoryConfiguration = configuration()) = (StartGeneration.parse(config) as StartGeneration.Result.Valid).command
