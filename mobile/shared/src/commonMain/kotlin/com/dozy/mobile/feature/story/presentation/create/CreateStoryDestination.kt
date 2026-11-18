package com.dozy.mobile.feature.story.presentation.create

import com.dozy.mobile.navigation.NavigationCommand

sealed interface CreateStoryDestination : NavigationCommand {
    data class Accepted(
        val jobId: String,
    ) : CreateStoryDestination

    data object OpenPlaces : CreateStoryDestination
}
