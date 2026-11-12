package com.dozy.mobile

import androidx.lifecycle.ViewModelStore
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.RefreshCharacters
import com.dozy.mobile.feature.home.presentation.HomeAction
import com.dozy.mobile.feature.home.presentation.HomeEffect
import com.dozy.mobile.feature.home.presentation.HomeSection
import com.dozy.mobile.feature.home.presentation.HomeViewModel
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.GenerationStatusDto
import com.dozy.mobile.feature.story.data.api.StorySummaryDto
import com.dozy.mobile.feature.story.data.toDomain
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.ObserveGenerations
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.RefreshGeneration
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import com.dozy.mobile.feature.story.domain.usecase.RetryGeneration
import com.dozy.mobile.feature.story.presentation.generation.GenerationAction
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
    private val repository = DefaultGenerationRepository(FakeGenerationDao(), api)
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

    @Test fun firstVisitWaitsForLoadingAndKeepsPredefinedPlaces() =
        runTest(dispatcher) {
            characters.characters.value = emptyList()
            places.places.value = listOf(place("builtin:woods", "Woods", false))
            val gate = CompletableDeferred<Unit>()
            characters.beforeRefresh = {
                gate.await()
                false
            }
            val vm = viewModel()
            assertFalse(vm.uiState.value.firstVisit)
            runCurrent()
            assertFalse(vm.uiState.value.firstVisit)
            assertEquals(
                "builtin:woods",
                vm.uiState.value.places
                    .single()
                    .id.value,
            )
            gate.complete(Unit)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.firstVisit)
            assertTrue(
                vm.uiState.value.loading
                    .isEmpty(),
            )
            characters.characters.value = listOf(character())
            runCurrent()
            assertFalse(vm.uiState.value.firstVisit)
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
                listOf(place("builtin:z", "Z", false), place("custom:z", "Z"), place("custom:a", "A"), place("builtin:a", "A", false))
            val vm = viewModel(stories = stories)
            advanceUntilIdle()
            assertEquals(
                listOf("story5", "story4", "story3"),
                vm.uiState.value.stories
                    .map { it.id },
            )
            assertEquals(6, vm.uiState.value.characters.size)
            assertEquals(
                listOf("custom:a", "custom:z", "builtin:a"),
                vm.uiState.value.places
                    .map { it.id.value },
            )
            val story =
                vm.uiState.value.stories
                    .first()
            vm.dispatch(HomeAction.StorySelected(story))
            assertEquals(HomeEffect.OpenStory(story), vm.effects.first())
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

    @Test fun failedObservationCanRestartWithoutClearingOtherSectionsOrShowingFirstVisit() =
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
            assertFalse(vm.uiState.value.firstVisit)
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
            api.remote = GenerationStatusDto("job", "READY", "story")
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
            assertEquals(HomeEffect.ReadStory("story"), vm.effects.first())
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

    @Test fun unavailableRetryRetainsFailedJobAndOffersAnotherAttempt() =
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
                    HomeAction.CreateStoryClicked to HomeEffect.CreateStoryRequested,
                    HomeAction.SeeAllStoriesClicked to HomeEffect.SeeAllStoriesRequested,
                    HomeAction.SeeAllCharactersClicked to HomeEffect.SeeAllCharactersRequested,
                    HomeAction.SeeAllPlacesClicked to HomeEffect.SeeAllPlacesRequested,
                    HomeAction.AddCharacterClicked to HomeEffect.AddCharacterRequested,
                    HomeAction.CharacterSelected(hero) to HomeEffect.OpenCharacter(hero),
                    HomeAction.PlaceSelected(setting) to HomeEffect.CreateStoryAtPlace(setting),
                    HomeAction.ProgressClicked("job") to HomeEffect.OpenProgress("job"),
                )
            for ((action, expected) in actions) {
                vm.resumed()
                advanceUntilIdle()
                repeat(3) { vm.dispatch(action) }
                assertEquals(expected, vm.effects.first())
                assertNull(withTimeoutOrNull(1) { vm.effects.first() })
            }
        }
}
