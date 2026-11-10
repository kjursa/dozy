package com.dozy.mobile.data.character

import com.dozy.mobile.data.character.api.CharacterDto
import com.dozy.mobile.data.character.database.CharacterEntity
import com.dozy.mobile.domain.character.Character
import com.dozy.mobile.domain.character.CharacterAppearance
import com.dozy.mobile.domain.character.CharacterDraft
import com.dozy.mobile.domain.character.CharacterId
import com.dozy.mobile.domain.character.CharacterName
import com.dozy.mobile.domain.character.CharacterPersonality
import com.dozy.mobile.domain.character.CharacterType

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
