package com.dozy.mobile

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.dozy.mobile.navigation.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GenerationNavigationTest {
    private fun controller() =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(ViewModelStore())
            graph =
                createGraph(startDestination = MAIN_ROUTE) {
                    composable(MAIN_ROUTE) { }
                    composable(GENERATION_ROUTE) { }
                    composable(READER_ROUTE) { }
                }
        }

    @Test fun notificationColdStartAndWarmStartOpenExactStoryWithHomeFallback() {
        val cold = controller()
        cold.openSavedStory("cold-story", fromNotification = true)
        assertEquals("cold-story", cold.currentBackStackEntry?.arguments?.getString("storyId"))
        cold.popBackStack()
        assertEquals(MAIN_ROUTE, cold.currentDestination?.route)

        val warm = controller()
        warm.navigate("generation/job")
        warm.openSavedStory("old-story")
        warm.openSavedStory("new-story", fromNotification = true)
        warm.openSavedStory("new-story", fromNotification = true)
        assertEquals("new-story", warm.currentBackStackEntry?.arguments?.getString("storyId"))
        warm.popBackStack()
        assertEquals(MAIN_ROUTE, warm.currentDestination?.route)
    }

    @Test fun readerLibraryReturnsToExistingMainAndSelectsLibrary() {
        val nav = controller()
        val main = nav.currentBackStackEntry
        nav.navigate("generation/job")
        nav.openSavedStory("story")
        nav.readerLibrary()
        assertSame(main, nav.currentBackStackEntry)
        assertEquals(true, main?.savedStateHandle?.get<Boolean>("showStories"))
    }

    @Test fun readyActionBackReturnsToProgressAndLeavingProgressReturnsHome() {
        val nav = controller()
        nav.navigate("generation/job")
        nav.openSavedStory("story")
        nav.openSavedStory("story")
        nav.popBackStack()
        assertEquals(GENERATION_ROUTE, nav.currentDestination?.route)
        assertEquals("job", nav.currentBackStackEntry?.arguments?.getString("jobId"))
        nav.popBackStack()
        assertEquals(MAIN_ROUTE, nav.currentDestination?.route)
    }
}
