package com.dozy.mobile.feature.character.data

import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.mobile.core.config.isAllowedAssetUrl
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput

fun Character.toEntity(pendingSync: Boolean) =
    CharacterEntity(
        id.value,
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
        pendingSync,
        avatar?.id?.value,
        avatar?.url?.value,
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
        if (avatarId != null &&
            avatarUrl != null
        ) {
            Character.Avatar(Character.Avatar.Id(avatarId), Character.Avatar.Url(avatarUrl))
        } else {
            null
        },
    )

fun Character.toDto() =
    CharacterDto(
        id.value,
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
        avatar?.let { AvatarDto(it.id.value, it.url.value) },
    )

fun CreateCharacterInput.toAvatarDto() = AvatarRequestDto(type.name, personality.value, appearance.value)

fun AvatarDto.toDomain(allowLocalHttp: Boolean = false): Character.Avatar {
    require(id.isNotBlank() && isAllowedAssetUrl(url, allowLocalHttp)) { "Malformed avatar asset" }
    return Character.Avatar(Character.Avatar.Id(id), Character.Avatar.Url(url))
}

fun CharacterDto.toDomain(allowLocalHttp: Boolean = false): Character {
    require(id.isNotBlank()) { "Malformed character" }
    return Character(
        Character.Id(id),
        Character.Details(
            Character.Name(name),
            Character.Personality(personality),
            Character.Appearance(appearance),
            Character.Type.valueOf(type),
        ),
        avatar?.toDomain(allowLocalHttp),
    )
}

fun Character.toSaveRequest() =
    SaveCharacterRequest(
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
        avatar?.let { AvatarDto(it.id.value, it.url.value) },
    )
