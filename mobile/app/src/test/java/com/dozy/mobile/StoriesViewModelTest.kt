package com.dozy.mobile

import com.dozy.mobile.feature.story.presentation.list.StoriesAction
import com.dozy.mobile.feature.story.presentation.list.StoriesEffect
import com.dozy.mobile.feature.story.presentation.list.StoriesViewModel
import com.kjursa.foundation.presentation.text.UiText
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
        assertEquals(UiText.Resource(R.string.my_stories), viewModel.uiState.value.title)
        assertEquals(
            listOf(
                UiText.Resource(R.string.oliver_and_the_secret_pirate_island),
                UiText.Resource(R.string.the_mystery_of_the_missing_moon),
                UiText.Resource(R.string.a_journey_through_the_enchanted_forest),
            ),
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
