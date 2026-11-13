package com.dozy.mobile.navigation

import androidx.navigation.NavHostController
import androidx.savedstate.read
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel

const val MAIN_ROUTE = "main"
const val CREATE_STORY_ROUTE = "create-story?place={place}&cast={cast}"
const val PLACES_ROUTE = "places?fromStory={fromStory}"
const val PLACE_FORM_ROUTE = "place-form?id={id}"
const val CREATE_CHARACTER_ROUTE = "create-character?id={id}"

/** Reader callers pass only cast IDs; previous plot and setting are deliberately absent. */
fun NavHostController.openStory(
    id: Place.Id?,
    cast: List<Character.Id> = emptyList(),
) {
    val parameters =
        buildList {
            id?.let { add("place=${encodeRouteArgument(it.value)}") }
            if (cast.isNotEmpty()) {
                add(
                    "cast=${encodeRouteArgument(
                        kotlinx.serialization.json.Json
                            .encodeToString(cast.map { it.value }),
                    )}",
                )
            }
        }
    navigate("create-story" + if (parameters.isEmpty()) "" else parameters.joinToString("&", "?")) { launchSingleTop = true }
}

const val GENERATION_ROUTE = "generation/{jobId}"

fun NavHostController.generationAccepted(jobId: String) {
    navigate("generation/${encodeRouteArgument(jobId)}") {
        popUpTo(CREATE_STORY_ROUTE) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavHostController.usePlace(
    id: Place.Id?,
    existingDraft: CreateStoryViewModel?,
) {
    if (existingDraft == null) {
        openStory(id)
    } else {
        existingDraft.dispatch(CreateStoryAction.Setting(id?.value))
        popBackStack()
    }
}

fun NavHostController.characterSaved(
    id: Character.Id,
    existingDraft: CreateStoryViewModel?,
) {
    if (existingDraft != null) {
        existingDraft.dispatch(CreateStoryAction.AddHero(id.value))
        popBackStack()
    } else {
        popBackStack()
        val destination = "character/${encodeRouteArgument(id.value)}"
        // Saving edits returns to the existing details entry rather than stacking another one.
        if (currentDestination?.route != "character/{id}" || currentBackStackEntry?.arguments?.read { getStringOrNull("id") } != id.value) {
            navigate(destination) { launchSingleTop = true }
        }
    }
}

/** Deletion always returns to the current character collection, even when entered from Home. */
fun NavHostController.characterDeleted() {
    getBackStackEntry(MAIN_ROUTE).savedStateHandle["showCharacters"] = true
    popBackStack(MAIN_ROUTE, inclusive = false)
}

const val READER_ROUTE = "reader/{storyId}"

fun NavHostController.openSavedStory(
    id: String,
    fromNotification: Boolean = false,
) {
    navigate("reader/${encodeRouteArgument(id)}") {
        if (fromNotification) popUpTo(MAIN_ROUTE) { inclusive = false }
        launchSingleTop = true
    }
}

fun NavHostController.readerLibrary() {
    getBackStackEntry(MAIN_ROUTE).savedStateHandle["showStories"] = true
    popBackStack(MAIN_ROUTE, inclusive = false)
}
