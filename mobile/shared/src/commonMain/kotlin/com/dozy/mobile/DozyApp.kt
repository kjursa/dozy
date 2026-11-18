package com.dozy.mobile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterScreen
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsScreen
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsViewModel
import com.dozy.mobile.feature.login.domain.repository.LoginRepository
import com.dozy.mobile.feature.login.presentation.LoginAction
import com.dozy.mobile.feature.login.presentation.LoginEffect
import com.dozy.mobile.feature.login.presentation.LoginEmailScreen
import com.dozy.mobile.feature.login.presentation.LoginPinScreen
import com.dozy.mobile.feature.login.presentation.LoginTutorialScreen
import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.dozy.mobile.feature.login.presentation.LoginWelcomeScreen
import com.dozy.mobile.feature.place.presentation.form.PlaceFormEffect
import com.dozy.mobile.feature.place.presentation.form.PlaceFormScreen
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.dozy.mobile.feature.place.presentation.list.PlacesAction
import com.dozy.mobile.feature.place.presentation.list.PlacesEffect
import com.dozy.mobile.feature.place.presentation.list.PlacesScreen
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryScreen
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.feature.story.presentation.generation.GenerationScreen
import com.dozy.mobile.feature.story.presentation.reader.ReaderEffect
import com.dozy.mobile.feature.story.presentation.reader.ReaderScreen
import com.dozy.mobile.navigation.CREATE_CHARACTER_ROUTE
import com.dozy.mobile.navigation.CREATE_STORY_ROUTE
import com.dozy.mobile.navigation.GENERATION_ROUTE
import com.dozy.mobile.navigation.LOGIN_EMAIL_ROUTE
import com.dozy.mobile.navigation.LOGIN_PIN_ROUTE
import com.dozy.mobile.navigation.LOGIN_TUTORIAL_ROUTE
import com.dozy.mobile.navigation.MAIN_ROUTE
import com.dozy.mobile.navigation.MainScreen
import com.dozy.mobile.navigation.PLACES_ROUTE
import com.dozy.mobile.navigation.PLACE_FORM_ROUTE
import com.dozy.mobile.navigation.READER_ROUTE
import com.dozy.mobile.navigation.characterDeleted
import com.dozy.mobile.navigation.characterSaved
import com.dozy.mobile.navigation.encodeRouteArgument
import com.dozy.mobile.navigation.generationAccepted
import com.dozy.mobile.navigation.openSavedStory
import com.dozy.mobile.navigation.openStory
import com.dozy.mobile.navigation.readerLibrary
import com.dozy.mobile.navigation.usePlace
import com.dozy.mobile.ui.theme.DozyTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DozyApp(
    notificationStory: String? = null,
    requestNotifications: (() -> Unit)? = null,
    backgroundCompletionSupported: Boolean = false,
    onNotificationHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    DozyTheme {
        val loginRepository: LoginRepository = koinInject()
        val loginViewModel: LoginViewModel = koinViewModel()
        val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()
        val startDestination =
            remember(loginViewModel) {
                if (loginRepository.hasSeenTutorial() || loginViewModel.uiState.value.tutorialSeen) {
                    "login-welcome"
                } else {
                    LOGIN_TUTORIAL_ROUTE
                }
            }
        LaunchedEffect(loginViewModel) {
            loginViewModel.effects.collect { effect ->
                when (effect) {
                    LoginEffect.OpenEmail ->
                        navController.navigate("login-welcome") {
                            popUpTo(LOGIN_TUTORIAL_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    LoginEffect.OpenPin -> navController.navigate(LOGIN_PIN_ROUTE) { launchSingleTop = true }
                    LoginEffect.OpenMain ->
                        navController.navigate(MAIN_ROUTE) {
                            popUpTo(startDestination) { inclusive = true }
                            launchSingleTop = true
                        }
                    LoginEffect.ReturnToEmail -> navController.popBackStack(LOGIN_EMAIL_ROUTE, inclusive = false)
                }
            }
        }
        LaunchedEffect(notificationStory, startDestination) {
            notificationStory?.let { storyId ->
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(startDestination) { inclusive = true }
                    launchSingleTop = true
                }
                navController.openSavedStory(storyId)
                onNotificationHandled()
            }
        }
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            composable(LOGIN_TUTORIAL_ROUTE) {
                LoginTutorialScreen { loginViewModel.dispatch(LoginAction.TutorialContinued) }
            }
            composable("login-welcome") {
                LoginWelcomeScreen(
                    onEmail = { navController.navigate(LOGIN_EMAIL_ROUTE) },
                    onGuest = { loginViewModel.dispatch(LoginAction.ContinueAsGuest) },
                )
            }
            composable(LOGIN_EMAIL_ROUTE) {
                LoginEmailScreen(
                    state = loginState,
                    onEmailChanged = { loginViewModel.dispatch(LoginAction.EmailChanged(it)) },
                    onSubmit = { loginViewModel.dispatch(LoginAction.EmailSubmitted) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(LOGIN_PIN_ROUTE) {
                LoginPinScreen(
                    state = loginState,
                    onBack = { loginViewModel.dispatch(LoginAction.BackToEmail) },
                    onResend = { loginViewModel.dispatch(LoginAction.ResendPin) },
                    onDigit = { loginViewModel.dispatch(LoginAction.PinDigit(it)) },
                    onDelete = { loginViewModel.dispatch(LoginAction.DeletePinDigit) },
                    onClear = { loginViewModel.dispatch(LoginAction.ClearPin) },
                )
            }
            composable(MAIN_ROUTE) { entry ->
                val showStories by entry.savedStateHandle.getStateFlow("showStories", false).collectAsStateWithLifecycle()
                val showCharacters by entry.savedStateHandle.getStateFlow("showCharacters", false).collectAsStateWithLifecycle()
                MainScreen(
                    showStories = showStories,
                    onStoriesShown = { entry.savedStateHandle["showStories"] = false },
                    showCharacters = showCharacters,
                    onCharactersShown = { entry.savedStateHandle["showCharacters"] = false },
                    onCreateStory = { navController.openStory(null) },
                    onCreateCharacter = { navController.navigate("create-character") },
                    onCharacter = { navController.navigate("character/${encodeRouteArgument(it)}") },
                    onPlaces = { navController.navigate("places?fromStory=false") { launchSingleTop = true } },
                    onPlaceSelected = { navController.openStory(it) },
                    onGeneration = { navController.navigate("generation/${encodeRouteArgument(it)}") { launchSingleTop = true } },
                    onRead = { navController.openSavedStory(it) },
                )
            }
            composable(CREATE_STORY_ROUTE) { entry ->
                val storyViewModel: CreateStoryViewModel = koinViewModel()
                LaunchedEffect(entry) {
                    entry.savedStateHandle.getStateFlow<String?>("newHero", null).collect { id ->
                        if (id != null) {
                            storyViewModel.dispatch(CreateStoryAction.AddHero(id))
                            entry.savedStateHandle["newHero"] = null
                        }
                    }
                }
                CreateStoryScreen(
                    viewModel = storyViewModel,
                    onBack = { navController.popBackStack() },
                    onNewHero = { navController.navigate("create-character") },
                    onChoosePlace = { navController.navigate("places?fromStory=true") { launchSingleTop = true } },
                    onGenerate = { jobId ->
                        navController.generationAccepted(jobId)
                    },
                )
            }
            composable(GENERATION_ROUTE) { entry ->
                GenerationScreen(
                    requireNotNull(entry.arguments?.read { getString("jobId") }),
                    onRead = { navController.openSavedStory(it) },
                    requestNotifications = requestNotifications,
                    backgroundCompletionSupported = backgroundCompletionSupported,
                    onHome = { navController.popBackStack(MAIN_ROUTE, inclusive = false) },
                )
            }
            composable(READER_ROUTE) { entry ->
                ReaderScreen { effect ->
                    when (effect) {
                        ReaderEffect.Back -> navController.popBackStack()
                        ReaderEffect.Library -> navController.readerLibrary()
                        is ReaderEffect.NewAdventure ->
                            navController.openStory(
                                null,
                                effect.cast,
                            )
                    }
                }
            }
            composable(PLACES_ROUTE) { entry ->
                val viewModel: PlacesViewModel = koinViewModel()
                val fromStory = entry.arguments?.read { getStringOrNull("fromStory") } == "true"
                val draft: CreateStoryViewModel? =
                    if (fromStory) {
                        koinViewModel(
                            viewModelStoreOwner = navController.getBackStackEntry(CREATE_STORY_ROUTE),
                        )
                    } else {
                        null
                    }
                PlacesScreen(viewModel) { effect ->
                    when (effect) {
                        PlacesEffect.Back -> navController.popBackStack()
                        is PlacesEffect.Form ->
                            navController.navigate(
                                if (effect.id == null) {
                                    "place-form"
                                } else {
                                    "place-form?id=${encodeRouteArgument(requireNotNull(effect.id).value)}"
                                },
                            ) { launchSingleTop = true }
                        is PlacesEffect.Selected -> navController.usePlace(effect.id, draft)
                    }
                }
            }
            composable(PLACE_FORM_ROUTE) {
                val viewModel: PlaceFormViewModel = koinViewModel()
                val list: PlacesViewModel = koinViewModel(viewModelStoreOwner = navController.getBackStackEntry(PLACES_ROUTE))
                PlaceFormScreen(viewModel) { effect ->
                    when (effect) {
                        PlaceFormEffect.Back -> navController.popBackStack()
                        is PlaceFormEffect.Saved -> {
                            list.dispatch(PlacesAction.Select(effect.id))
                            navController.popBackStack()
                        }
                    }
                }
            }
            composable("character/{id}") {
                val vm: CharacterDetailsViewModel = koinViewModel()
                CharacterDetailsScreen(
                    vm,
                    onDeleted = { navController.characterDeleted() },
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("create-character?id=${encodeRouteArgument(it.value)}") },
                    onStory = {
                        navController.openStory(null, listOf(it))
                    },
                )
            }
            composable(CREATE_CHARACTER_ROUTE) {
                val characterViewModel: CreateCharacterViewModel = koinViewModel()
                val draft: CreateStoryViewModel? =
                    if (navController.previousBackStackEntry?.destination?.route == CREATE_STORY_ROUTE) {
                        koinViewModel(viewModelStoreOwner = navController.getBackStackEntry(CREATE_STORY_ROUTE))
                    } else {
                        null
                    }
                CreateCharacterScreen(
                    viewModel = characterViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { id -> navController.characterSaved(id, draft) },
                )
            }
        }
    }
}
