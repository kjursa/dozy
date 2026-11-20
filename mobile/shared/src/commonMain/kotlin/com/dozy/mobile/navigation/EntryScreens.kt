package com.dozy.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.savedstate.read
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterScreen
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsScreen
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsViewModel
import com.dozy.mobile.feature.login.presentation.LoginAction
import com.dozy.mobile.feature.login.presentation.LoginEmailScreen
import com.dozy.mobile.feature.login.presentation.LoginPinScreen
import com.dozy.mobile.feature.login.presentation.LoginTutorialScreen
import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.dozy.mobile.feature.login.presentation.LoginWelcomeScreen
import com.dozy.mobile.feature.place.presentation.form.PlaceFormScreen
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.dozy.mobile.feature.place.presentation.list.PlacesScreen
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryScreen
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.feature.story.presentation.generation.GenerationScreen
import com.dozy.mobile.feature.story.presentation.reader.ReaderScreen
import com.kjursa.foundation.compose.navigation.Screen
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Each route resolves its ViewModel using the current navigation entry. */
class TutorialEntryScreen(
    private val loginViewModel: LoginViewModel,
) : Screen {
    @Composable
    override fun Content() {
        LoginTutorialScreen { loginViewModel.dispatch(LoginAction.TutorialContinued) }
    }
}

class WelcomeEntryScreen(
    private val navigator: AppNavigator,
    private val loginViewModel: LoginViewModel,
) : Screen {
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()

        LoginWelcomeScreen(
            onEmail = { scope.launch { navigator.navigate(HostDestination.Email) } },
            onGuest = { loginViewModel.dispatch(LoginAction.ContinueAsGuest) },
        )
    }
}

class EmailEntryScreen(
    private val loginViewModel: LoginViewModel,
) : Screen {
    @Composable
    override fun Content() {
        val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()

        LoginEmailScreen(
            state = loginState,
            onEmailChanged = { loginViewModel.dispatch(LoginAction.EmailChanged(it)) },
            onSubmit = { loginViewModel.dispatch(LoginAction.EmailSubmitted) },
        )
    }
}

class PinEntryScreen(
    private val loginViewModel: LoginViewModel,
) : Screen {
    @Composable
    override fun Content() {
        val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()
        LoginPinScreen(
            state = loginState,
            onResend = { loginViewModel.dispatch(LoginAction.ResendPin) },
            onDigit = { loginViewModel.dispatch(LoginAction.PinDigit(it)) },
            onDelete = { loginViewModel.dispatch(LoginAction.DeletePinDigit) },
            onClear = { loginViewModel.dispatch(LoginAction.ClearPin) },
        )
    }
}

class MainEntryScreen : Screen {
    @Composable
    override fun Content() {
        val entry = LocalViewModelStoreOwner.current as NavBackStackEntry

        val showStories by entry.savedStateHandle.getStateFlow("showStories", false).collectAsStateWithLifecycle()
        val showCharacters by entry.savedStateHandle.getStateFlow("showCharacters", false).collectAsStateWithLifecycle()
        MainScreen(
            showStories = showStories,
            onStoriesShown = { entry.savedStateHandle["showStories"] = false },
            showCharacters = showCharacters,
            onCharactersShown = { entry.savedStateHandle["showCharacters"] = false },
        )
    }
}

class CreateStoryEntryScreen(
    private val navigator: AppNavigator,
) : Screen {
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val entry = LocalViewModelStoreOwner.current as NavBackStackEntry

        val storyViewModel: CreateStoryViewModel = koinViewModel()
        LaunchedEffect(entry) {
            entry.savedStateHandle.getStateFlow<String?>("newHero", null).collect { id ->
                if (id != null) {
                    storyViewModel.dispatch(CreateStoryAction.AddHero(id))
                    entry.savedStateHandle["newHero"] = null
                }
            }
        }
        LaunchedEffect(entry, storyViewModel) {
            entry.savedStateHandle.getStateFlow<String?>("selectedPlace", null).collect { value ->
                if (value != null) {
                    storyViewModel.dispatch(CreateStoryAction.Setting(value.takeIf { it.isNotEmpty() }))
                    entry.savedStateHandle["selectedPlace"] = null
                }
            }
        }
        CreateStoryScreen(
            viewModel = storyViewModel,
            onBack = { scope.launch { navigator.navigate(HostDestination.Back) } },
            onNewHero = { scope.launch { navigator.navigate(HostDestination.NewCharacter) } },
        )
    }
}

class GenerationEntryScreen(
    private val navigator: AppNavigator,
    private val options: NavigationHostOptions,
) : Screen {
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val entry = LocalViewModelStoreOwner.current as NavBackStackEntry

        GenerationScreen(
            requireNotNull(entry.arguments?.read { getString("jobId") }),
            requestNotifications = options.requestNotifications,
            backgroundCompletionSupported = options.backgroundCompletionSupported,
            onHome = { scope.launch { navigator.navigate(HostDestination.Home) } },
        )
    }
}

class ReaderEntryScreen : Screen {
    @Composable override fun Content() {
        ReaderScreen()
    }
}

class PlacesEntryScreen : Screen {
    @Composable override fun Content() {
        val viewModel: PlacesViewModel = koinViewModel()
        PlacesScreen(viewModel)
    }
}

class PlaceFormEntryScreen : Screen {
    @Composable override fun Content() {
        val viewModel: PlaceFormViewModel = koinViewModel()
        PlaceFormScreen(viewModel)
    }
}

class CharacterEntryScreen : Screen {
    @Composable override fun Content() {
        val viewModel: CharacterDetailsViewModel = koinViewModel()
        CharacterDetailsScreen(viewModel)
    }
}

class CreateCharacterEntryScreen : Screen {
    @Composable override fun Content() {
        val viewModel: CreateCharacterViewModel = koinViewModel()
        CreateCharacterScreen(viewModel)
    }
}
