package com.dozy.mobile

import com.dozy.mobile.domain.*
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
) = com.dozy.mobile.domain.Character(
    CharacterId(id),
    validDraft.copy(name = CharacterName(name)),
)

internal class FakeCharacterRepository : CharacterRepository {
    val characters = MutableStateFlow(listOf(character(), character("2", "Luna"), character("3", "Captain Finn")))
    var failure: Exception? = null
    var saves = 0
    var beforeSave: suspend () -> Unit = {}

    override fun observe() = characters

    override suspend fun save(character: com.dozy.mobile.domain.Character): SavedCharacter {
        saves++
        beforeSave()
        failure?.let { throw it }
        characters.value = characters.value + character
        return SavedCharacter(character, CharacterSync.PENDING)
    }
}
