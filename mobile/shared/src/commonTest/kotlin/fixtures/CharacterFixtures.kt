package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
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
) = Character(
    Character.Id(id),
    validDetails.copy(name = Character.Name(name)),
)

internal class FakeCharacterRepository : CharacterRepository {
    val characters = MutableStateFlow(listOf(character(), character("2", "Luna"), character("3", "Captain Finn")))
    var failure: Exception? = null
    var refreshes = 0
    var beforeRefresh: suspend () -> Boolean = { false }

    override suspend fun refresh(): Boolean {
        refreshes++
        return beforeRefresh()
    }

    var deletes = 0
    var beforeDelete: suspend () -> Unit = {}

    override suspend fun delete(id: Character.Id): CharacterSync {
        deletes++
        beforeDelete()
        failure?.let { throw it }
        characters.value = characters.value.filterNot { it.id == id }
        return CharacterSync.PENDING
    }

    var saves = 0
    var beforeSave: suspend () -> Unit = {}

    override fun observe() = characters

    override suspend fun save(character: Character): SavedCharacter {
        saves++
        beforeSave()
        failure?.let { throw it }
        characters.value = characters.value.filterNot { it.id == character.id } + character
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

fun avatar() = Character.Avatar(Character.Avatar.Id("asset"), Character.Avatar.Url("https://example.test/avatar.png"))
