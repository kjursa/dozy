package com.dozy.mobile.feature.character.data

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.delay

/** Local preview generator; every persistence operation still uses the real repository. */
internal class FakeAvatarGenerator(
    private val repository: CharacterRepository,
) : CharacterRepository by repository {
    override suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar {
        delay(750)
        val asset =
            when (input.type) {
                Character.Type.CHILD, Character.Type.OTHER -> "character_oliver"
                Character.Type.ANIMAL -> "character_luna"
                Character.Type.FANTASY -> "character_finn"
            }
        return Character.Avatar(
            Character.Avatar.Id("fake:$asset"),
            Character.Avatar.Url("bundled-avatar://$asset"),
        )
    }
}
