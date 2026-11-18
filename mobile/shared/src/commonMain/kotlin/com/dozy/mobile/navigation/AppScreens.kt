package com.dozy.mobile.navigation

/** Flow-level dependencies shared by screen bindings. */
data class NavigationHostOptions(
    val requestNotifications: (() -> Unit)?,
    val backgroundCompletionSupported: Boolean,
)

/** Injected route entries; no route-specific ViewModels are retained here. */
class AppScreens(
    val tutorial: TutorialEntryScreen,
    val welcome: WelcomeEntryScreen,
    val email: EmailEntryScreen,
    val pin: PinEntryScreen,
    val main: MainEntryScreen,
    val createStory: CreateStoryEntryScreen,
    val generation: GenerationEntryScreen,
    val reader: ReaderEntryScreen,
    val places: PlacesEntryScreen,
    val placeForm: PlaceFormEntryScreen,
    val character: CharacterEntryScreen,
    val createCharacter: CreateCharacterEntryScreen,
)
