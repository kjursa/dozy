package com.dozy.mobile

import com.dozy.mobile.presentation.stories.StoriesAction
import com.dozy.mobile.presentation.stories.StoriesEffect
import com.dozy.mobile.presentation.stories.StoriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoriesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun initialStateExposesSampleStories() {
        val viewModel = StoriesViewModel()
        assertEquals("My Stories", viewModel.uiState.value.title)
        assertEquals(
            listOf("Oliver and the Secret Pirate Island", "The Mystery of the Missing Moon", "A Journey Through the Enchanted Forest"),
            viewModel.uiState.value.stories
                .map { it.title },
        )
    }

    @Test fun selectingStoryEmitsOpenStoryEffect() =
        runTest(dispatcher) {
            val viewModel = StoriesViewModel()
            val story =
                viewModel.uiState.value.stories
                    .first()
            val effect = async { viewModel.effects.first() }
            viewModel.dispatch(StoriesAction.StorySelected(story))
            advanceUntilIdle()
            assertEquals(StoriesEffect.OpenStory(story), effect.await())
        }
}
