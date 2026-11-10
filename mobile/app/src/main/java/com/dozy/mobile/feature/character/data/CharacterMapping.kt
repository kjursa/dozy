package com.dozy.mobile.feature.character.data

import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.domain.Character
import com.dozy.mobile.feature.character.domain.CharacterAppearance
import com.dozy.mobile.feature.character.domain.CharacterDraft
import com.dozy.mobile.feature.character.domain.CharacterId
import com.dozy.mobile.feature.character.domain.CharacterName
import com.dozy.mobile.feature.character.domain.CharacterPersonality
import com.dozy.mobile.feature.character.domain.CharacterType

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
        CharacterId(id),
        CharacterDraft(
            CharacterName(name),
            CharacterPersonality(personality),
            CharacterAppearance(appearance),
            CharacterType.valueOf(type),
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
