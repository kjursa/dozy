package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.Character
import com.dozy.mobile.feature.character.domain.CharacterAppearance
import com.dozy.mobile.feature.character.domain.CharacterDraft
import com.dozy.mobile.feature.character.domain.CharacterId
import com.dozy.mobile.feature.character.domain.CharacterName
import com.dozy.mobile.feature.character.domain.CharacterPersonality
import com.dozy.mobile.feature.character.domain.CharacterRepository
import com.dozy.mobile.feature.character.domain.CharacterSync
import com.dozy.mobile.feature.character.domain.CharacterType
import com.dozy.mobile.feature.character.domain.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.SavedCharacter
import kotlinx.coroutines.flow.MutableStateFlow

internal val validDraft =
    CharacterDraft(
        CharacterName("Oliver"),
        CharacterPersonality("Curious and brave"),
        CharacterAppearance("Curly brown hair"),
        CharacterType.CHILD,
    )

internal fun character(
    id: String = "1",
    name: String = "Oliver",
) = com.dozy.mobile.feature.character.domain.Character(
    CharacterId(id),
    validDraft.copy(name = CharacterName(name)),
)

internal class FakeCharacterRepository : CharacterRepository {
    val characters = MutableStateFlow(listOf(character(), character("2", "Luna"), character("3", "Captain Finn")))
    var failure: Exception? = null
    var saves = 0
    var beforeSave: suspend () -> Unit = {}

    override fun observe() = characters

    override suspend fun save(character: com.dozy.mobile.feature.character.domain.Character): SavedCharacter {
        saves++
        beforeSave()
        failure?.let { throw it }
        characters.value = characters.value + character
        return SavedCharacter(character, CharacterSync.PENDING)
    }
}

internal fun createInput(name: String = "Oliver"): CreateCharacterInput =
    (CreateCharacterInput.from(name, "Curious and brave", "Curly brown hair", CharacterType.CHILD) as CreateCharacterValidation.Valid).input
