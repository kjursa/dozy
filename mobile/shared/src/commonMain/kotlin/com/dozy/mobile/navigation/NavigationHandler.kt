package com.dozy.mobile.navigation

import androidx.navigation.NavHostController
import androidx.savedstate.read
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterDestination
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsDestination
import com.dozy.mobile.feature.character.presentation.list.CharactersDestination
import com.dozy.mobile.feature.home.presentation.HomeDestination
import com.dozy.mobile.feature.launcher.presentation.LauncherDestination
import com.dozy.mobile.feature.login.presentation.LoginDestination
import com.dozy.mobile.feature.place.presentation.form.PlaceFormDestination
import com.dozy.mobile.feature.place.presentation.list.PlacesDestination
import com.dozy.mobile.feature.story.presentation.create.CreateStoryDestination
import com.dozy.mobile.feature.story.presentation.generation.GenerationDestination
import com.dozy.mobile.feature.story.presentation.list.StoriesDestination
import com.dozy.mobile.feature.story.presentation.reader.ReaderDestination
import com.dozy.mobile.navigation.CREATE_STORY_ROUTE
import com.dozy.mobile.navigation.HostDestination
import com.dozy.mobile.navigation.LAUNCHER_ROUTE
import com.dozy.mobile.navigation.LOGIN_EMAIL_ROUTE
import com.dozy.mobile.navigation.LOGIN_PIN_ROUTE
import com.dozy.mobile.navigation.LOGIN_TUTORIAL_ROUTE
import com.dozy.mobile.navigation.LOGIN_WELCOME_ROUTE
import com.dozy.mobile.navigation.MAIN_ROUTE
import com.dozy.mobile.navigation.characterDeleted
import com.dozy.mobile.navigation.characterSaved
import com.dozy.mobile.navigation.encodeRouteArgument
import com.dozy.mobile.navigation.generationAccepted
import com.dozy.mobile.navigation.openSavedStory
import com.dozy.mobile.navigation.openStory
import com.dozy.mobile.navigation.readerLibrary

/** The application's only consumer of navigation commands. */
fun NavHostController.handleNavigation(command: NavigationCommand) {
    when (command) {
        LauncherDestination.Home ->
            navigate(MAIN_ROUTE) {
                popUpTo(LAUNCHER_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        LauncherDestination.Welcome ->
            navigate(LOGIN_WELCOME_ROUTE) {
                popUpTo(LAUNCHER_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        LauncherDestination.Tutorial ->
            navigate(LOGIN_TUTORIAL_ROUTE) {
                popUpTo(LAUNCHER_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        LauncherDestination.Email ->
            navigate(LOGIN_EMAIL_ROUTE) {
                popUpTo(LAUNCHER_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        HostDestination.Back -> popBackStack()
        HostDestination.Email -> navigate(LOGIN_EMAIL_ROUTE)
        HostDestination.NewCharacter -> navigate("create-character")
        HostDestination.Home -> popBackStack(MAIN_ROUTE, false)
        LoginDestination.OpenEmail ->
            navigate("login-welcome") {
                popUpTo(LOGIN_TUTORIAL_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        LoginDestination.OpenPin -> navigate(LOGIN_PIN_ROUTE) { launchSingleTop = true }
        LoginDestination.OpenMain ->
            navigate(MAIN_ROUTE) {
                popUpTo(graph.id) { inclusive = true }
                launchSingleTop = true
            }
        LoginDestination.ReturnToEmail -> popBackStack(LOGIN_EMAIL_ROUTE, false)
        HomeDestination.CreateStoryRequested, StoriesDestination.CreateStory -> openStory(null)
        HomeDestination.AddCharacterRequested, CharactersDestination.NavigateToAddCharacter ->
            navigate(
                "create-character",
            )
        is HomeDestination.OpenCharacter -> navigate("character/${encodeRouteArgument(command.character.id)}")
        is CharactersDestination.OpenCharacter ->
            navigate(
                "character/${encodeRouteArgument(command.character.id)}",
            )
        is HomeDestination.OpenStory -> openSavedStory(command.story.id)
        is StoriesDestination.OpenStory -> openSavedStory(command.story.id)
        is HomeDestination.ReadStory -> openSavedStory(command.id)
        is GenerationDestination.Read -> openSavedStory(command.id.value)
        HomeDestination.SeeAllStoriesRequested, ReaderDestination.Library -> readerLibrary()
        HomeDestination.SeeAllCharactersRequested -> {
            getBackStackEntry(MAIN_ROUTE).savedStateHandle["showCharacters"] = true
        }
        HomeDestination.SeeAllPlacesRequested -> navigate("places?fromStory=false")
        is HomeDestination.CreateStoryAtPlace -> openStory(command.id)
        is HomeDestination.OpenProgress -> navigate("generation/${encodeRouteArgument(command.id)}")
        CreateStoryDestination.OpenPlaces -> navigate("places?fromStory=true")
        is CreateStoryDestination.Accepted -> generationAccepted(command.jobId)
        ReaderDestination.Back, PlacesDestination.Back, PlaceFormDestination.Back,
        CharacterDetailsDestination.Back, CreateCharacterDestination.NavigateBack,
        -> popBackStack()
        is ReaderDestination.NewAdventure -> openStory(null, command.cast)
        is PlacesDestination.Form ->
            navigate(
                command.id?.let { "place-form?id=${encodeRouteArgument(it.value)}" } ?: "place-form",
            ) { launchSingleTop = true }
        is PlacesDestination.Selected -> {
            val fromStory = currentBackStackEntry?.arguments?.read { getStringOrNull("fromStory") } == "true"
            if (fromStory) {
                getBackStackEntry(CREATE_STORY_ROUTE).savedStateHandle["selectedPlace"] = command.id?.value ?: ""
                popBackStack()
            } else {
                openStory(command.id)
            }
        }
        is PlaceFormDestination.Saved -> {
            popBackStack()
            handleNavigation(PlacesDestination.Selected(command.id))
        }
        CharacterDetailsDestination.Deleted -> characterDeleted()
        is CharacterDetailsDestination.Edit ->
            navigate(
                "create-character?id=${encodeRouteArgument(command.id.value)}",
            )
        is CharacterDetailsDestination.Story -> openStory(null, listOf(command.id))
        is CreateCharacterDestination.Saved -> {
            if (previousBackStackEntry?.destination?.route == CREATE_STORY_ROUTE) {
                previousBackStackEntry?.savedStateHandle?.set("newHero", command.id.value)
                popBackStack()
            } else {
                characterSaved(command.id, null)
            }
        }
    }
}
