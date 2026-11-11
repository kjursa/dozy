package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.di.characterModule
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterAction
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterEffect
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.list.CharactersViewModel
import com.dozy.mobile.feature.home.di.homeModule
import com.dozy.mobile.feature.home.presentation.HomeViewModel
import com.dozy.mobile.feature.story.di.storyModule
import com.dozy.mobile.feature.story.presentation.list.StoriesViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
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
                    single { ApiConfig(baseUrl = null) }
                    single<CharacterDao> { dao }
                    single<com.dozy.mobile.feature.story.data.database.GenerationDao> { FakeGenerationDao() }
                    single<HttpClient> { client }
                    single<CharacterIdGenerator> { CharacterIdGenerator { Character.Id("injected-id") } }
                }
            val app = koinApplication { modules(characterModule, homeModule, storyModule, platformModule) }
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
                        parametersOf(
                            SavedStateHandle(
                                mapOf(
                                    SavedStatePersistence.DEFAULT_KEY to
                                        Json.encodeToString(
                                            CreateCharacterUiState.serializer(),
                                            CreateCharacterUiState(
                                                name = "Koin hero",
                                                personality = "Curious",
                                                appearance = "Green eyes",
                                                avatarId = "asset",
                                                avatarUrl = "https://example.test/avatar.png",
                                            ),
                                        ),
                                ),
                            ),
                        )
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
                assertEquals(CreateCharacterEffect.Saved(Character.Id("injected-id")), form.effects.first())
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
                val repository = app.koin.get<CharacterRepository>()
                val original = repository.observe().first().single()
                val edited = original.copy(details = original.details.copy(name = Character.Name("Edited hero")))
                repository.update(edited)
                advanceUntilIdle()
                assertEquals(edited, repository.observe(edited.id).first())
                assertEquals(
                    "Edited hero",
                    home.uiState.value.characters
                        .single()
                        .name,
                )
                assertEquals(
                    "Edited hero",
                    characters.uiState.value.characters
                        .single()
                        .name,
                )
                assertEquals(1, dao.rows.value.size)
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

        override suspend fun pendingIds() = rows.value.filter { it.pendingSync }.map { it.id }

        override suspend fun deleteSynced() {
            rows.value = rows.value.filter { it.pendingSync }
        }

        override fun observe() = rows.map { it.filterNot { row -> row.deleted } }

        override fun observe(id: String) = rows.map { it.firstOrNull { row -> row.id == id && !row.deleted } }

        override suspend fun deletedIds() = rows.value.filter { it.deleted }.map { it.id }

        override suspend fun markDeleted(id: String) {
            rows.value = rows.value.map { if (it.id == id) it.copy(deleted = true, pendingSync = true) else it }
        }

        override suspend fun updateExisting(
            id: String,
            name: String,
            personality: String,
            appearance: String,
            type: String,
            avatarId: String?,
            avatarUrl: String?,
        ): Int {
            if (rows.value.none { it.id == id && !it.deleted }) return 0
            upsert(CharacterEntity(id, name, personality, appearance, type, true, avatarId, avatarUrl))
            return 1
        }

        override suspend fun upsert(character: CharacterEntity) {
            rows.value = rows.value.filterNot { it.id == character.id } + character
        }
    }
}
