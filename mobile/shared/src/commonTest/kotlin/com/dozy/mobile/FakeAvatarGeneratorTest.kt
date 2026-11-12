package com.dozy.mobile

import com.dozy.mobile.feature.character.data.FakeAvatarGenerator
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAvatarGeneratorTest {
    @Test fun generatesBundledAvatarsWithoutSavingOrCallingTheRemoteGenerator() =
        runTest {
            val repository = LocalRepository()
            val generator = FakeAvatarGenerator(repository)
            val expected =
                mapOf(
                    Character.Type.CHILD to "character_oliver",
                    Character.Type.OTHER to "character_oliver",
                    Character.Type.ANIMAL to "character_luna",
                    Character.Type.FANTASY to "character_finn",
                )
            for ((type, asset) in expected) {
                val result = generator.generateAvatar(input(type))
                assertEquals("bundled-avatar://$asset", result.url.value)
                assertEquals("fake:$asset", result.id.value)
            }
            assertTrue(repository.rows.value.isEmpty())
        }

    @Test fun generationSuspendsAndCanBeCancelledWithoutSaving() =
        runTest {
            val repository = LocalRepository()
            val generation = async { FakeAvatarGenerator(repository).generateAvatar(input()) }
            runCurrent()
            advanceTimeBy(749)
            assertFalse(generation.isCompleted)
            generation.cancel()
            runCurrent()
            assertTrue(generation.isCancelled)
            assertTrue(repository.rows.value.isEmpty())
        }

    @Test fun explicitSaveDelegatesToTheRepositoryAndBundledReferenceSurvivesStorageMapping() =
        runTest {
            val repository = LocalRepository()
            val generator = FakeAvatarGenerator(repository)
            val avatar = generator.generateAvatar(input())
            val character = character().copy(avatar = avatar)
            assertEquals(character, character.toEntity(true).toDomain())
            assertEquals(SavedCharacter(character, CharacterSync.PENDING), generator.save(character))
            assertEquals(listOf(character), repository.rows.value)
        }

    private fun input(type: Character.Type = Character.Type.CHILD) =
        (CreateCharacterInput.from("Oliver", "Curious", "Green eyes", type) as CreateCharacterValidation.Valid).input

    private class LocalRepository : CharacterRepository {
        val rows = MutableStateFlow<List<Character>>(emptyList())

        override fun observe() = rows

        override suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar? =
            error("Fake must not request remote generation")

        override suspend fun delete(id: Character.Id): CharacterSync = error("Unexpected deletion")

        override suspend fun save(character: Character): SavedCharacter {
            rows.value = rows.value + character
            return SavedCharacter(character, CharacterSync.PENDING)
        }
    }
}
