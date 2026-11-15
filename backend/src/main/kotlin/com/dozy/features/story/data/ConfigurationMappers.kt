package com.dozy.features.story.data

import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.features.story.domain.model.AvatarSnapshot
import com.dozy.features.story.domain.model.HeroSnapshot
import com.dozy.features.story.domain.model.PlaceSnapshot
import com.dozy.features.story.domain.model.StoryConfiguration

internal fun GenerationRequestDto.toDomain() = StoryConfiguration(
    cast.map { HeroSnapshot(it.id, it.name, it.personality, it.appearance, it.type, it.avatar?.let { avatar -> AvatarSnapshot(avatar.id, avatar.url) }) },
    StoryConfiguration.Mode.valueOf(mode), StoryConfiguration.Length.valueOf(length),
    place?.let { PlaceSnapshot(it.id, it.name, it.description, it.origin, it.editable, it.inhabitants) }, idea, ending,
)
internal fun StoryConfiguration.toDto() = GenerationRequestDto(
    cast.map { CharacterDto(it.id, it.name, it.personality, it.appearance, it.type, it.avatar?.let { avatar -> AvatarDto(avatar.id, avatar.url) }) },
    mode.name, length.name,
    place?.let { PlaceResponse(it.id, it.name, it.description, it.origin, it.editable, it.inhabitants) }, idea, ending,
)
