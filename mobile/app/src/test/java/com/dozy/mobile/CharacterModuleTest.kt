package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.dozy.mobile.data.character.database.CharacterDao
import com.dozy.mobile.data.character.database.CharacterEntity
import com.dozy.mobile.di.characterModule
import com.dozy.mobile.domain.character.CharacterId
import com.dozy.mobile.domain.character.CharacterIdGenerator
import com.dozy.mobile.domain.character.CharacterRepository
import com.dozy.mobile.presentation.characters.CharactersViewModel
import com.dozy.mobile.presentation.createcharacter.CreateCharacterAction
import com.dozy.mobile.presentation.createcharacter.CreateCharacterEffect
import com.dozy.mobile.presentation.createcharacter.CreateCharacterViewModel
import com.dozy.mobile.presentation.home.HomeViewModel
import com.dozy.mobile.presentation.stories.StoriesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.resolveViewModel

// Exercise the same owner-based resolver used by koinViewModel; internal API is confined to this test.
@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
class CharacterModuleTest {
    @Test fun commonGraphUsesPlatformDependenciesAndScopesViewModelsToTheirOwner() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val dao = InMemoryDao()
            val client = HttpClient(MockEngine { error("Sync must remain disabled") })
            val platformModule =
                module {
                    single<CharacterDao> { dao }
                    single<HttpClient> { client }
                    single<CharacterIdGenerator> { CharacterIdGenerator { CharacterId("injected-id") } }
                }
            val app = koinApplication { modules(characterModule, platformModule) }
            val owner = ViewModelStore()
            val otherOwner = ViewModelStore()
            try {
                fun storiesViewModel(store: ViewModelStore) =
                    resolveViewModel<StoriesViewModel>(
                        vmClass = StoriesViewModel::class,
                        viewModelStore = store,
                        extras = CreationExtras.Empty,
                        scope = app.koin.scopeRegistry.rootScope,
                    )
                val stories = storiesViewModel(owner)
                assertSame(stories, storiesViewModel(owner))
                assertNotSame(stories, storiesViewModel(otherOwner))
                val form =
                    app.koin.get<CreateCharacterViewModel> {
                        parametersOf(SavedStateHandle(mapOf("createCharacter.name" to "Koin hero")))
                    }
                assertSame(app.koin.get<CharacterRepository>(), app.koin.get<CharacterRepository>())
                val home =
                    resolveViewModel(HomeViewModel::class, owner, extras = CreationExtras.Empty, scope = app.koin.scopeRegistry.rootScope)
                val characters =
                    resolveViewModel(
                        CharactersViewModel::class,
                        owner,
                        extras = CreationExtras.Empty,
                        scope = app.koin.scopeRegistry.rootScope,
                    )
                resolveViewModel(StoriesViewModel::class, owner, extras = CreationExtras.Empty, scope = app.koin.scopeRegistry.rootScope)
                form.dispatch(CreateCharacterAction.SaveClicked)
                advanceUntilIdle()
                assertEquals(CreateCharacterEffect.Saved, form.effects.first())
                assertEquals(
                    "injected-id",
                    dao.rows.value
                        .single()
                        .id,
                )
                assertEquals(
                    "Koin hero",
                    home.uiState.value.characters
                        .single()
                        .name,
                )
                assertEquals(
                    "Koin hero",
                    characters.uiState.value.characters
                        .single()
                        .name,
                )
            } finally {
                owner.clear()
                otherOwner.clear()
                app.close()
                client.close()
                Dispatchers.resetMain()
            }
        }

    private class InMemoryDao : CharacterDao {
        val rows = MutableStateFlow(emptyList<CharacterEntity>())

        override fun observe() = rows

        override suspend fun upsert(character: CharacterEntity) {
            rows.value = rows.value.filterNot { it.id == character.id } + character
        }
    }
}
