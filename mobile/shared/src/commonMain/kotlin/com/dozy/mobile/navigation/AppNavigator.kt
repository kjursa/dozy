package com.dozy.mobile.navigation

import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.kjursa.foundation.presentation.navigation.Navigator
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/** Commands carry data only; exactly one application host consumes this queue. */
interface NavigationCommand

sealed interface HostDestination : NavigationCommand {
    data object Back : HostDestination

    data object Email : HostDestination

    data object NewCharacter : HostDestination

    data object Home : HostDestination
}

typealias AppNavigator = Navigator<NavigationCommand>

val navigationModule =
    module {
        factory { HomeTabScreen() }
        factory { StoriesTabEntry() }
        factory { CharactersTabEntry() }
        factory { parameters -> TutorialEntryScreen(parameters.get()) }
        factory { parameters -> WelcomeEntryScreen(get(), parameters.get()) }
        factory { parameters -> EmailEntryScreen(parameters.get()) }
        factory { parameters -> PinEntryScreen(parameters.get()) }
        factory { MainEntryScreen() }
        factory { CreateStoryEntryScreen(get()) }
        factory { parameters -> GenerationEntryScreen(get(), parameters.get()) }
        factory { ReaderEntryScreen() }
        factory { PlacesEntryScreen() }
        factory { PlaceFormEntryScreen() }
        factory { CharacterEntryScreen() }
        factory { CreateCharacterEntryScreen() }
        factory { parameters ->
            val login: LoginViewModel = parameters.get()
            val options: NavigationHostOptions = parameters.get()
            AppScreens(
                tutorial = get { parametersOf(login) },
                welcome = get { parametersOf(login) },
                email = get { parametersOf(login) },
                pin = get { parametersOf(login) },
                main = get(),
                createStory = get(),
                generation = get { parametersOf(options) },
                reader = get(),
                places = get(),
                placeForm = get(),
                character = get(),
                createCharacter = get(),
            )
        }
        single<AppNavigator> { Navigator() }
    }
