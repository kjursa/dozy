package com.dozy.mobile.feature.story.presentation.create

internal sealed interface CreateStoryEffect {
    data class Accepted(
        val jobId: String,
    ) : CreateStoryEffect

    data object OpenPlaces : CreateStoryEffect
}
