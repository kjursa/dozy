package com.dozy.mobile.navigation

import android.net.Uri
import androidx.navigation.NavHostController
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel

internal const val MAIN_ROUTE = "main"
internal const val CREATE_STORY_ROUTE = "create-story?place={place}"
internal const val PLACES_ROUTE = "places?fromStory={fromStory}"
internal const val PLACE_FORM_ROUTE = "place-form?id={id}"
internal const val CREATE_CHARACTER_ROUTE = "create-character?id={id}"

internal fun NavHostController.openStory(id: Place.Id?) {
    navigate(if (id == null) "create-story" else "create-story?place=${Uri.encode(id.value)}") { launchSingleTop = true }
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
