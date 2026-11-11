package com.dozy.mobile

import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryUiState
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.navigation.CREATE_CHARACTER_ROUTE
import com.dozy.mobile.navigation.CREATE_STORY_ROUTE
import com.dozy.mobile.navigation.MAIN_ROUTE
import com.dozy.mobile.navigation.PLACES_ROUTE
import com.dozy.mobile.navigation.PLACE_FORM_ROUTE
import com.dozy.mobile.navigation.characterSaved
import com.dozy.mobile.navigation.openStory
import com.dozy.mobile.navigation.usePlace
import com.kjursa.foundation.compose.mvi.StatePersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PlaceNavigationTest {
    private fun controller() =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(ViewModelStore())
            graph =
                createGraph(startDestination = MAIN_ROUTE) {
                    composable(MAIN_ROUTE) { }
                    composable(CREATE_CHARACTER_ROUTE) { }
                    composable("character/{id}") { }
                    composable(CREATE_STORY_ROUTE) { }
                    composable(PLACES_ROUTE) { }
                    composable(PLACE_FORM_ROUTE) { }
                }
        }

    @Test fun homeSelectionStartsStoryAndPreservesPlacesAsBackDestination() {
        val nav = controller()
        nav.navigate("places?fromStory=false")
        val places = nav.currentBackStackEntry
        nav.navigate("place-form")
        nav.popBackStack()
        nav.usePlace(Place.Id("builtin:woods"), null)
        assertEquals(CREATE_STORY_ROUTE, nav.currentDestination?.route)
        assertEquals("builtin:woods", nav.currentBackStackEntry?.arguments?.getString("place"))
        nav.popBackStack()
        assertSame(places, nav.currentBackStackEntry)
        nav.popBackStack()
        assertEquals(MAIN_ROUTE, nav.currentDestination?.route)
    }

    @Test fun storySelectionReturnsToSameEntryAndFormBackKeepsOriginAndInput() {
        val nav = controller()
        nav.openStory(null)
        val story = nav.currentBackStackEntry
        val draft =
            CreateStoryViewModel(
                object : StatePersistence<CreateStoryUiState> {
                    override fun restore(defaultState: CreateStoryUiState) = defaultState

                    override fun save(state: CreateStoryUiState) = Unit
                },
                null,
                SelectPlace(FakePlaceRepository()),
                ObserveCharacters(FakeCharacterRepository()),
            )
        draft.dispatch(CreateStoryAction.Request("A moonlit adventure"))
        val input = draft.uiState.value
        nav.navigate("places?fromStory=true")
        val places = nav.currentBackStackEntry
        nav.navigate("place-form?id=custom")
        assertEquals("custom", nav.currentBackStackEntry?.arguments?.getString("id"))
        nav.popBackStack()
        assertSame(places, nav.currentBackStackEntry)
        assertEquals("true", places?.arguments?.getString("fromStory"))
        nav.popBackStack()
        assertSame(story, nav.currentBackStackEntry)
        assertEquals(input, draft.uiState.value)
        nav.navigate("places?fromStory=true")
        nav.navigate("place-form")
        nav.popBackStack()
        nav.usePlace(Place.Id("custom"), draft)
        assertSame(story, nav.currentBackStackEntry)
        assertEquals("custom", draft.uiState.value.placeId)
        assertEquals(input.specialRequest, draft.uiState.value.specialRequest)
        assertEquals(input.selectedHeroes, draft.uiState.value.selectedHeroes)
        nav.navigate("places?fromStory=true")
        nav.usePlace(null, draft)
        assertSame(story, nav.currentBackStackEntry)
        assertTrue(draft.uiState.value.randomSetting)
        assertEquals(false, draft.uiState.value.automaticPlot)
    }

    @Test fun savedCharacterFromHomeOpensItsDetailsAndEditsReturnToSameEntry() {
        val nav = controller()
        nav.navigate("create-character")
        nav.characterSaved(Character.Id("new"), null)
        assertEquals("character/{id}", nav.currentDestination?.route)
        assertEquals("new", nav.currentBackStackEntry?.arguments?.getString("id"))
        val details = nav.currentBackStackEntry
        nav.navigate("create-character?id=new")
        nav.characterSaved(Character.Id("new"), null)
        assertSame(details, nav.currentBackStackEntry)
        nav.popBackStack()
        assertEquals(MAIN_ROUTE, nav.currentDestination?.route)
    }

    @Test fun characterSaveSelectsNewHeroAndReturnsToSameStoryDraft() {
        val nav = controller()
        nav.openStory(null)
        val story = nav.currentBackStackEntry
        val draft =
            CreateStoryViewModel(
                object : StatePersistence<CreateStoryUiState> {
                    override fun restore(defaultState: CreateStoryUiState) = defaultState

                    override fun save(state: CreateStoryUiState) = Unit
                },
                null,
                SelectPlace(FakePlaceRepository()),
                ObserveCharacters(FakeCharacterRepository()),
            )
        draft.dispatch(CreateStoryAction.Request("Moonlit adventure"))
        draft.dispatch(CreateStoryAction.Ending("Back home"))
        draft.dispatch(CreateStoryAction.Length(1))
        draft.dispatch(CreateStoryAction.AddHero("existing"))
        val before = draft.uiState.value
        nav.navigate("create-character")
        nav.popBackStack()
        assertEquals(before, draft.uiState.value)
        nav.navigate("create-character")
        nav.characterSaved(Character.Id("new"), draft)
        assertSame(story, nav.currentBackStackEntry)
        assertEquals(before.copy(selectedHeroes = listOf("existing", "new")), draft.uiState.value)
        draft.dispatch(CreateStoryAction.AddHero("new"))
        assertEquals(listOf("existing", "new"), draft.uiState.value.selectedHeroes)
    }
}
