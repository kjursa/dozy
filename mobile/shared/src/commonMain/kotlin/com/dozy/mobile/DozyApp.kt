package com.dozy.mobile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dozy.api.client.auth.AuthSessionManager
import com.dozy.api.client.auth.SessionState
import com.dozy.mobile.feature.launcher.presentation.LauncherDestination
import com.dozy.mobile.feature.launcher.presentation.LauncherScreen
import com.dozy.mobile.feature.launcher.presentation.LauncherViewModel
import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.AppScreens
import com.dozy.mobile.navigation.CREATE_CHARACTER_ROUTE
import com.dozy.mobile.navigation.CREATE_STORY_ROUTE
import com.dozy.mobile.navigation.GENERATION_ROUTE
import com.dozy.mobile.navigation.LAUNCHER_ROUTE
import com.dozy.mobile.navigation.LOGIN_EMAIL_ROUTE
import com.dozy.mobile.navigation.LOGIN_PIN_ROUTE
import com.dozy.mobile.navigation.LOGIN_TUTORIAL_ROUTE
import com.dozy.mobile.navigation.LOGIN_WELCOME_ROUTE
import com.dozy.mobile.navigation.MAIN_ROUTE
import com.dozy.mobile.navigation.NavigationHostOptions
import com.dozy.mobile.navigation.PLACES_ROUTE
import com.dozy.mobile.navigation.PLACE_FORM_ROUTE
import com.dozy.mobile.navigation.READER_ROUTE
import com.dozy.mobile.navigation.handleNavigation
import com.dozy.mobile.navigation.openSavedStory
import com.dozy.mobile.ui.theme.DozyTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DozyApp(
    notificationStory: String? = null,
    requestNotifications: (() -> Unit)? = null,
    backgroundCompletionSupported: Boolean = false,
    onNotificationHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    DozyTheme {
        val loginViewModel: LoginViewModel = koinViewModel()
        val navigator: AppNavigator = koinInject()
        val sessionManager: AuthSessionManager = koinInject()
        var startupResolved by remember { mutableStateOf(false) }
        LaunchedEffect(sessionManager, navController) {
            sessionManager.state.collect { state ->
                when (state) {
                    SessionState.Expired ->
                        navController.navigate(LOGIN_EMAIL_ROUTE) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    else -> Unit
                }
            }
        }
        LaunchedEffect(navigator, navController) {
            navigator.commands.collect { command ->
                navController.handleNavigation(command)
                if (command is LauncherDestination) startupResolved = true
            }
        }
        LaunchedEffect(notificationStory, startupResolved) {
            if (!startupResolved) return@LaunchedEffect
            notificationStory?.let { storyId ->
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
                navController.openSavedStory(storyId)
                onNotificationHandled()
            }
        }
        val screens: AppScreens =
            koinInject {
                parametersOf(loginViewModel, NavigationHostOptions(requestNotifications, backgroundCompletionSupported))
            }
        NavHost(
            navController = navController,
            startDestination = LAUNCHER_ROUTE,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            composable(LAUNCHER_ROUTE) {
                val launcherViewModel: LauncherViewModel = koinViewModel()
                LauncherScreen(launcherViewModel)
            }
            composable(LOGIN_TUTORIAL_ROUTE) { screens.tutorial.Content() }
            composable(LOGIN_WELCOME_ROUTE) { screens.welcome.Content() }
            composable(LOGIN_EMAIL_ROUTE) { screens.email.Content() }
            composable(LOGIN_PIN_ROUTE) { screens.pin.Content() }
            composable(MAIN_ROUTE) { screens.main.Content() }
            composable(CREATE_STORY_ROUTE) { screens.createStory.Content() }
            composable(GENERATION_ROUTE) { screens.generation.Content() }
            composable(READER_ROUTE) { screens.reader.Content() }
            composable(PLACES_ROUTE) { screens.places.Content() }
            composable(PLACE_FORM_ROUTE) { screens.placeForm.Content() }
            composable("character/{id}") { screens.character.Content() }
            composable(CREATE_CHARACTER_ROUTE) { screens.createCharacter.Content() }
        }
    }
}
