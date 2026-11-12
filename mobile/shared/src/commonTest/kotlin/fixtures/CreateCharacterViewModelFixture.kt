package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.SavedCharacter
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.*
import com.dozy.mobile.feature.character.presentation.create.*
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
internal open class CreateCharacterViewModelFixture {
    protected val dispatcher = StandardTestDispatcher()
    protected val stored = FakeCharacterRepository()
    protected var readFailure: Exception? = null
    protected var generations = 0
    protected var generate: suspend () -> Character.Avatar? = { avatar() }
    protected val repository =
        object : CharacterRepository {
            override suspend fun delete(
                id: com.dozy.mobile.feature.character.domain.model.Character.Id,
            ): com.dozy.mobile.feature.character.domain.model.CharacterSync = error("Unexpected deletion")

            override fun observe() =
                flow {
                    readFailure?.let { throw it }
                    emitAll(stored.observe())
                }

            override suspend fun save(character: Character): SavedCharacter = stored.save(character)

            override suspend fun generateAvatar(input: CreateCharacterInput): Character.Avatar? {
                generations++
                assertEquals("Curious", input.personality.value)
                return generate()
            }
        }

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest fun teardown() {
        Dispatchers.resetMain()
    }

    protected suspend fun TestScope.ready(vm: CreateCharacterViewModel) {
        vm.dispatch(CreateCharacterAction.PreviewClicked)
        runCurrent()
    }

    protected fun fill(vm: CreateCharacterViewModel) {
        vm.dispatch(CreateCharacterAction.NameChanged("Finn"))
        vm.dispatch(CreateCharacterAction.PersonalityChanged("Curious"))
        vm.dispatch(CreateCharacterAction.AppearanceChanged("Green eyes"))
    }

    protected fun vm(
        handle: SavedStateHandle = SavedStateHandle(),
        id: String? = null,
    ) = CreateCharacterViewModel(
        SavedStatePersistence(handle, CreateCharacterUiState.serializer()),
        SaveCharacter(repository, CharacterIdGenerator { Character.Id("new") }),
        GenerateCharacterAvatar(repository),
        ObserveCharacter(repository),
        id,
    )
}
