package com.dozy.mobile

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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun initialStateContainsHomeHeaderAndSampleCollections() {
        val state = HomeViewModel().uiState.value

        assertEquals("TUESDAY, 14 OCTOBER", state.dateLabel)
        assertEquals("Good evening!", state.greeting)
        assertEquals("Ready for a new adventure?", state.prompt)
        assertEquals(
            listOf("Oliver and the Secret Pirate Island", "The Mystery of the Missing Moon", "A Journey Through the Enchanted Forest"),
            state.stories.map { it.title },
        )
        assertEquals(listOf("Oliver", "Luna", "Captain Finn"), state.characters.map { it.name })
    }

    @Test fun homeActionsEmitExpectedEffects() = runTest(dispatcher) {
        val viewModel = HomeViewModel()
        val story = viewModel.uiState.value.stories.first()
        val character = viewModel.uiState.value.characters.first()
        val expectedEffects = listOf(
            HomeEffect.CreateStoryRequested,
            HomeEffect.OpenStory(story),
            HomeEffect.SeeAllStoriesRequested,
            HomeEffect.OpenCharacter(character),
            HomeEffect.AddCharacterRequested,
            HomeEffect.OpenNotifications,
        )
        val effects = async { expectedEffects.map { viewModel.effects.first() } }

        listOf(
            HomeAction.CreateStoryClicked,
            HomeAction.StorySelected(story),
            HomeAction.SeeAllStoriesClicked,
            HomeAction.CharacterSelected(character),
            HomeAction.AddCharacterClicked,
            HomeAction.NotificationsClicked,
        ).forEach { action ->
            viewModel.dispatch(action)
            advanceUntilIdle()
        }

        assertEquals(expectedEffects, effects.await())
    }
}
