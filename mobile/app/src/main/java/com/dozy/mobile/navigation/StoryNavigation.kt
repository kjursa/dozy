package com.dozy.mobile.navigation

import android.net.Uri
import androidx.navigation.NavHostController
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel

internal const val MAIN_ROUTE = "main"
internal const val CREATE_STORY_ROUTE = "create-story?place={place}&cast={cast}"
internal const val PLACES_ROUTE = "places?fromStory={fromStory}"
internal const val PLACE_FORM_ROUTE = "place-form?id={id}"
internal const val CREATE_CHARACTER_ROUTE = "create-character?id={id}"

/** Reader callers pass only cast IDs; previous plot and setting are deliberately absent. */
internal fun NavHostController.openStory(
    id: Place.Id?,
    cast: List<Character.Id> = emptyList(),
) {
    val parameters =
        buildList {
            id?.let { add("place=${Uri.encode(it.value)}") }
            if (cast.isNotEmpty()) {
                add(
                    "cast=${Uri.encode(
                        kotlinx.serialization.json.Json
                            .encodeToString(cast.map { it.value }),
                    )}",
                )
            }
        }
    navigate("create-story" + if (parameters.isEmpty()) "" else parameters.joinToString("&", "?")) { launchSingleTop = true }
}

internal const val GENERATION_ROUTE = "generation/{jobId}"

internal fun NavHostController.generationAccepted(jobId: String) {
    navigate("generation/${Uri.encode(jobId)}") {
        popUpTo(CREATE_STORY_ROUTE) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun NavHostController.usePlace(
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

internal fun NavHostController.characterSaved(
    id: Character.Id,
    existingDraft: CreateStoryViewModel?,
) {
    if (existingDraft != null) {
        existingDraft.dispatch(CreateStoryAction.AddHero(id.value))
        popBackStack()
    } else {
        popBackStack()
        val destination = "character/${Uri.encode(id.value)}"
        // Saving edits returns to the existing details entry rather than stacking another one.
        if (currentDestination?.route != "character/{id}" || currentBackStackEntry?.arguments?.getString("id") != id.value) {
            navigate(destination) { launchSingleTop = true }
        }
    }
}
