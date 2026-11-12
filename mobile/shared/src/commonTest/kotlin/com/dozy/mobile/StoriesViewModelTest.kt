package com.dozy.mobile

import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.domain.usecase.*
import com.dozy.mobile.feature.story.presentation.list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
class StoriesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun loadingRefreshEmptyErrorOfflineAndRetryAreDistinctAndRepeatedRefreshIsIgnored() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            var calls = 0
            var fail = false
            var offline = false
            val api =
                object : GenerationApi by ProgressApi() {
                    override suspend fun stories(cursor: String?): StoryPageDto? {
                        calls++
                        gate.await()
                        if (fail) error("Unavailable")
                        return if (offline) null else StoryPageDto(emptyList())
                    }
                }
            val repo = DefaultGenerationRepository(FakeGenerationDao(), api)
            val vm = StoriesViewModel(ObserveStoryLibrary(repo), RefreshStories(repo))
            assertTrue(vm.uiState.value.loading)
            runCurrent()
            assertFalse(vm.uiState.value.loading)
            assertTrue(vm.uiState.value.refreshing)
            repeat(3) { vm.dispatch(StoriesAction.Refresh) }
            runCurrent()
            assertEquals(1, calls)
            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.refreshing)
            assertTrue(
                vm.uiState.value.stories
                    .isEmpty(),
            )
            assertNull(vm.uiState.value.error)
            fail = true
            vm.dispatch(StoriesAction.Refresh)
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.error)
            fail = false
            offline = true
            vm.dispatch(StoriesAction.Refresh)
            advanceUntilIdle()
            assertNull(vm.uiState.value.error)
            assertTrue(vm.uiState.value.offline)
            offline = false
            vm.dispatch(StoriesAction.Refresh)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.offline)
        }

    @Test fun completedStoryUpdatesLibraryAndNavigationIsOneOffAndCanResume() =
        runTest(dispatcher) {
            val api = ProgressApi()
            val repo = DefaultGenerationRepository(FakeGenerationDao(), api)
            val vm = StoriesViewModel(ObserveStoryLibrary(repo), RefreshStories(repo))
            advanceUntilIdle()
            repo.start(
                com.dozy.mobile.feature.story.domain.model.GenerationJob
                    .RequestKey("key"),
                api.configuration,
            )
            api.remote = GenerationStatusDto("job", "READY", "story")
            repo.refresh(
                com.dozy.mobile.feature.story.domain.model.GenerationJob
                    .Id("job"),
            )
            advanceUntilIdle()
            val story =
                vm.uiState.value.stories
                    .single()
            assertEquals("story", story.id)
            repeat(3) { vm.dispatch(StoriesAction.StorySelected(story)) }
            assertEquals(StoriesEffect.OpenStory(story), vm.effects.first())
            assertTrue(vm.uiState.value.navigating)
            vm.navigationHandled()
            vm.dispatch(StoriesAction.Create)
            assertEquals(StoriesEffect.CreateStory, vm.effects.first())
        }
}
