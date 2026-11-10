package com.dozy.mobile.feature.character.data

import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.domain.Character

fun Character.toEntity(pendingSync: Boolean) =
    CharacterEntity(
        id.value,
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
        pendingSync,
    )

fun CharacterEntity.toDomain() =
    Character(
        Character.Id(id),
        Character.Details(
            Character.Name(name),
            Character.Personality(personality),
            Character.Appearance(appearance),
            Character.Type.valueOf(type),
        ),
    )

fun Character.toDto() =
    CharacterDto(
        id.value,
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
    )
