package com.dozy.mobile

import androidx.lifecycle.ViewModelStore
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.RefreshCharacters
import com.dozy.mobile.feature.home.presentation.HomeAction
import com.dozy.mobile.feature.home.presentation.HomeDestination
import com.dozy.mobile.feature.home.presentation.HomeSection
import com.dozy.mobile.feature.home.presentation.HomeViewModel
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.story.data.testGenerationRepository
import com.dozy.mobile.feature.story.data.toDomain
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.ObserveGenerations
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.ReconcileGenerations
import com.dozy.mobile.feature.story.domain.usecase.RefreshGeneration
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import com.dozy.mobile.feature.story.domain.usecase.RetryGeneration
import com.dozy.mobile.feature.story.presentation.generation.GenerationAction
import com.kjursa.foundation.presentation.navigation.Navigator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val characters = FakeCharacterRepository()
    private val places = FakePlaceRepository()
    private val api = ProgressApi()
    private val repository = testGenerationRepository(FakeGenerationDao(), api)
    private val jobId = GenerationJob.Id("job")

    @BeforeTest fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(
        heroes: CharacterRepository = characters,
        stories: GenerationRepository = repository,
    ) = HomeViewModel(
        Navigator(),
        ObserveCharacters(heroes),
        ObserveStoryLibrary(stories),
        ObservePlaces(places),
        ObserveGenerations(stories),
        RefreshCharacters(heroes),
        RefreshStories(stories),
        RefreshPlaces(places),
        RefreshGeneration(stories),
        RetryGeneration(stories, GenerationKeyGenerator { GenerationJob.RequestKey("retry") }),
    ).also { store.put("home", it) }

    @Test fun emptyHomeKeepsPlacesVisibleDuringCharacterRefresh() =
        runTest(dispatcher) {
            characters.characters.value = emptyList()
            places.places.value = listOf(place("place-00000000-0000-7000-8000-000000000001", "Woods", false))
            val gate = CompletableDeferred<Unit>()
            characters.beforeRefresh = {
                gate.await()
                false
            }
            val vm = viewModel()
            runCurrent()
            assertEquals(
                "place-00000000-0000-7000-8000-000000000001",
                vm.uiState.value.places
                    .single()
                    .id.value,
            )
            gate.complete(Unit)
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.loading
                    .isEmpty(),
            )
            val refreshGate = CompletableDeferred<Unit>()
            characters.beforeRefresh = {
                refreshGate.await()
                false
            }
            vm.resumed()
            runCurrent()
            assertTrue(HomeSection.CHARACTERS in vm.uiState.value.refreshing)
            assertTrue(
                vm.uiState.value.characters
                    .isEmpty(),
            )
            assertTrue(
                vm.uiState.value.stories
                    .isEmpty(),
            )
            assertEquals(
                "place-00000000-0000-7000-8000-000000000001",
                vm.uiState.value.places
                    .single()
                    .id.value,
            )
            refreshGate.complete(Unit)
            advanceUntilIdle()

            characters.characters.value = listOf(character())
            runCurrent()
            assertEquals(
                "Oliver",
                vm.uiState.value.characters
                    .single()
                    .name,
            )
            assertTrue(
                vm.uiState.value.stories
                    .isEmpty(),
            )
        }

    @Test fun sectionsComposeWithPreviewLimitsAndStableLibraryAndPlaceOrder() =
        runTest(dispatcher) {
            val summaries = (1..5).map { StorySummaryDto("story$it", "Title $it", "https://example.test/cover", 3, it.toLong()) }
            val stories =
                object : GenerationRepository by repository {
                    override fun summaries() = flowOf(summaries.reversed().map { it.toDomain() })
                }
            characters.characters.value = (1..8).map { character("hero$it", "Hero $it") }
            places.places.value =
                listOf(
                    place("place-00000000-0000-7000-8000-000000000003", "Z", false),
                    place("custom:z", "Z"),
                    place("custom:a", "A"),
                    place("place-00000000-0000-7000-8000-000000000002", "A", false),
                )
            val vm = viewModel(stories = stories)
            advanceUntilIdle()
            assertEquals(
                listOf("story5", "story4", "story3"),
                vm.uiState.value.stories
                    .map { it.id },
            )
            assertEquals(6, vm.uiState.value.characters.size)
            assertEquals(
                listOf("custom:a", "custom:z", "place-00000000-0000-7000-8000-000000000002"),
                vm.uiState.value.places
                    .map { it.id.value },
            )
            val story =
                vm.uiState.value.stories
                    .first()
            vm.dispatch(HomeAction.StorySelected(story))
            assertEquals(HomeDestination.OpenStory(story), vm.navigator.commands.first())
        }

    @Test fun refreshFailuresAreIndependentRetainContentAndRetryIsGuarded() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            val known = vm.uiState.value.characters
            characters.beforeRefresh = { error("Unavailable") }
            vm.resumed()
            advanceUntilIdle()
            assertEquals(setOf(HomeSection.CHARACTERS), vm.uiState.value.errors.keys)
            assertEquals(known, vm.uiState.value.characters)
            assertEquals(2, vm.uiState.value.places.size)
            val gate = CompletableDeferred<Unit>()
            characters.beforeRefresh = {
                gate.await()
                true
            }
            val calls = characters.refreshes
            repeat(3) { vm.dispatch(HomeAction.RetrySection(HomeSection.CHARACTERS)) }
            runCurrent()
            assertEquals(calls + 1, characters.refreshes)
            gate.complete(Unit)
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.errors
                    .isEmpty(),
            )
        }

    @Test fun storyAndPlaceErrorsDoNotHideCharactersAndCanRecoverIndependently() =
        runTest(dispatcher) {
            var failStories = false
            val stories =
                object : GenerationRepository by repository {
                    override suspend fun refreshStories(): Boolean {
                        if (failStories) error("Stories unavailable")
                        return false
                    }
                }
            val vm = viewModel(stories = stories)
            advanceUntilIdle()
            val knownPlaces = vm.uiState.value.places
            failStories = true
            places.refreshBlock = { error("Places unavailable") }
            vm.resumed()
            advanceUntilIdle()
            assertEquals(setOf(HomeSection.STORIES, HomeSection.PLACES), vm.uiState.value.errors.keys)
            assertEquals(3, vm.uiState.value.characters.size)
            assertEquals(knownPlaces, vm.uiState.value.places)
            failStories = false
            vm.dispatch(HomeAction.RetrySection(HomeSection.STORIES))
            advanceUntilIdle()
            assertEquals(setOf(HomeSection.PLACES), vm.uiState.value.errors.keys)
            places.refreshBlock = { true }
            vm.dispatch(HomeAction.RetrySection(HomeSection.PLACES))
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.errors
                    .isEmpty(),
            )
        }

    @Test fun failedObservationCanRestartWithoutClearingOtherSections() =
        runTest(dispatcher) {
            var failRead = true
            val heroes =
                object : CharacterRepository by characters {
                    override fun observe() =
                        flow {
                            if (failRead) error("Read failed")
                            emitAll(characters.observe())
                        }
                }
            val vm = viewModel(heroes)
            advanceUntilIdle()
            assertTrue(HomeSection.CHARACTERS in vm.uiState.value.errors)
            assertTrue(
                vm.uiState.value.places
                    .isNotEmpty(),
            )
            failRead = false
            vm.dispatch(HomeAction.RetrySection(HomeSection.CHARACTERS))
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.errors
                    .isEmpty(),
            )
            assertEquals(3, vm.uiState.value.characters.size)
        }

    @Test fun resumeRefreshesProgressAndReadyStoryIsInRecentStories() =
        runTest(dispatcher) {
            repository.start(GenerationJob.RequestKey("key"), api.configuration)
            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(
                GenerationJob.Status.QUEUED,
                vm.uiState.value.generation.homeJob
                    ?.status,
            )
            api.remote = api.ready()
            vm.resumed()
            advanceUntilIdle()
            assertEquals(
                GenerationJob.Status.READY,
                vm.uiState.value.generation.homeJob
                    ?.status,
            )
            assertEquals(
                "story",
                vm.uiState.value.stories
                    .single()
                    .id,
            )
            vm.dispatch(HomeAction.Generation(GenerationAction.Open(jobId)))
            assertEquals(HomeDestination.ReadStory("story"), vm.navigator.commands.first())
        }

    @Test fun failedJobRetryRetainsContentAndBlocksRepeatedClicks() =
        runTest(dispatcher) {
            repository.start(GenerationJob.RequestKey("key"), api.configuration)
            api.remote = GenerationStatusDto("job", "FAILED", errorCode = "GENERATION_FAILED")
            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(
                GenerationJob.Status.FAILED,
                vm.uiState.value.generation.homeJob
                    ?.status,
            )
            api.retryGate = CompletableDeferred()
            repeat(3) { vm.dispatch(HomeAction.Generation(GenerationAction.Retry(jobId))) }
            runCurrent()
            assertTrue(vm.uiState.value.generation.busy)
            assertEquals(1, api.retryKeys.size)
            assertEquals(3, vm.uiState.value.characters.size)
            api.retryGate!!.complete(Unit)
            advanceUntilIdle()
            assertEquals(
                GenerationJob.Status.QUEUED,
                vm.uiState.value.generation.homeJob
                    ?.status,
            )
            assertFalse(vm.uiState.value.generation.busy)
        }

    @Test fun unavailableRetryRetainsKeyAndBlocksTapsUntilBackgroundReconciliation() =
        runTest(dispatcher) {
            repository.start(GenerationJob.RequestKey("key"), api.configuration)
            api.remote = GenerationStatusDto("job", "FAILED", errorCode = "GENERATION_FAILED")
            val vm = viewModel()
            advanceUntilIdle()
            api.loseRetryResponse = true
            vm.dispatch(HomeAction.Generation(GenerationAction.Retry(jobId)))
            advanceUntilIdle()
            assertTrue(HomeSection.GENERATION in vm.uiState.value.errors)
            assertFalse(vm.uiState.value.generation.busy)
            assertTrue(
                vm.uiState.value.generation.homeJob!!
                    .retryPending,
            )
            assertTrue(
                vm.uiState.value.places
                    .isNotEmpty(),
            )
            api.loseRetryResponse = false
            vm.dispatch(HomeAction.Generation(GenerationAction.Retry(jobId)))
            advanceUntilIdle()
            assertEquals(listOf("retry"), api.retryKeys)
            ReconcileGenerations(
                repository,
                RetryGeneration(repository, GenerationKeyGenerator { GenerationJob.RequestKey("unused") }),
            ) { true }()
            vm.dispatch(HomeAction.Generation(GenerationAction.Refresh(jobId)))
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.errors
                    .isEmpty(),
            )
            assertEquals(
                GenerationJob.Status.QUEUED,
                vm.uiState.value.generation.homeJob
                    ?.status,
            )
            assertEquals(listOf("retry", "retry"), api.retryKeys)
        }

    @Test fun navigationEffectsUseSelectedIdsAndRepeatedClicksAreIgnoredUntilResume() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            val hero =
                vm.uiState.value.characters
                    .first()
            val setting =
                vm.uiState.value.places
                    .first()
                    .id
            val actions =
                listOf(
                    HomeAction.CreateStoryClicked to HomeDestination.CreateStoryRequested,
                    HomeAction.SeeAllStoriesClicked to HomeDestination.SeeAllStoriesRequested,
                    HomeAction.SeeAllCharactersClicked to HomeDestination.SeeAllCharactersRequested,
                    HomeAction.SeeAllPlacesClicked to HomeDestination.SeeAllPlacesRequested,
                    HomeAction.AddCharacterClicked to HomeDestination.AddCharacterRequested,
                    HomeAction.CharacterSelected(hero) to HomeDestination.OpenCharacter(hero),
                    HomeAction.PlaceSelected(setting) to HomeDestination.CreateStoryAtPlace(setting),
                    HomeAction.ProgressClicked("job") to HomeDestination.OpenProgress("job"),
                )
            for ((action, expected) in actions) {
                vm.resumed()
                advanceUntilIdle()
                repeat(3) { vm.dispatch(action) }
                assertEquals(expected, vm.navigator.commands.first())
                assertNull(withTimeoutOrNull(1) { vm.navigator.commands.first() })
            }
        }
}
