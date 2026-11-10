package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.Character
import com.dozy.mobile.feature.character.domain.CharacterRepository
import com.dozy.mobile.feature.character.domain.CharacterSync
import com.dozy.mobile.feature.character.domain.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.SavedCharacter
import kotlinx.coroutines.flow.MutableStateFlow

internal val validDetails =
    Character.Details(
        Character.Name("Oliver"),
        Character.Personality("Curious and brave"),
        Character.Appearance("Curly brown hair"),
        Character.Type.CHILD,
    )

internal fun character(
    id: String = "1",
    name: String = "Oliver",
) = com.dozy.mobile.feature.character.domain.Character(
    Character.Id(id),
    validDetails.copy(name = Character.Name(name)),
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
    (
        CreateCharacterInput.from(
            name,
            "Curious and brave",
            "Curly brown hair",
            Character.Type.CHILD,
        ) as CreateCharacterValidation.Valid
    ).input
