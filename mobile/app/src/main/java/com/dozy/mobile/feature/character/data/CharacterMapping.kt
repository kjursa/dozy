package com.dozy.mobile.feature.character.data

import com.dozy.mobile.feature.character.data.api.AvatarDto
import com.dozy.mobile.feature.character.data.api.AvatarRequestDto
import com.dozy.mobile.feature.character.data.api.CharacterDto
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import io.ktor.http.URLProtocol
import io.ktor.http.Url

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

fun AvatarDto.toDomain(): Character.Avatar {
    val parsed = Url(url)
    require(id.isNotBlank() && parsed.protocol == URLProtocol.HTTPS && parsed.host.isNotBlank()) { "Malformed avatar asset" }
    return Character.Avatar(Character.Avatar.Id(id), Character.Avatar.Url(url))
}

fun CharacterDto.toDomain(): Character {
    require(id.isNotBlank() && name.isNotBlank()) { "Malformed character" }
    return Character(
        Character.Id(id),
        Character.Details(
            Character.Name(name),
            Character.Personality(personality),
            Character.Appearance(appearance),
            Character.Type.valueOf(type),
        ),
        avatar?.toDomain(),
    )
}
