package com.dozy.features.story.data

import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.domain.id.AssetId
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.PlaceId
import com.dozy.features.story.domain.model.AvatarSnapshot
import com.dozy.features.story.domain.model.HeroSnapshot
import com.dozy.features.story.domain.model.PlaceSnapshot
import com.dozy.features.story.domain.model.StoryConfiguration

internal fun GenerationRequestDto.toDomain(): StoryConfiguration? {
    val heroes = cast.map { hero ->
        val id = CharacterId.parse(hero.id) ?: return null
        val avatar = hero.avatar?.let {
            AvatarSnapshot(AssetId.parse(it.id) ?: return null, it.url)
        }
        HeroSnapshot(id, hero.name, hero.personality, hero.appearance, hero.type, avatar)
    }
    val placeSnapshot = place?.let {
        PlaceSnapshot(PlaceId.parse(it.id) ?: return null, it.name, it.description, it.origin, it.editable, it.inhabitants)
    }
    return StoryConfiguration(heroes, StoryConfiguration.Mode.valueOf(mode), StoryConfiguration.Length.valueOf(length), placeSnapshot, idea, ending)
}

internal fun StoryConfiguration.toDto() = GenerationRequestDto(
    cast.map { CharacterDto(it.id.value, it.name, it.personality, it.appearance, it.type, it.avatar?.let { avatar -> AvatarDto(avatar.id.value, avatar.url) }) },
    mode.name, length.name,
    place?.let { PlaceResponse(it.id.value, it.name, it.description, it.origin, it.editable, it.inhabitants) }, idea, ending,
)
