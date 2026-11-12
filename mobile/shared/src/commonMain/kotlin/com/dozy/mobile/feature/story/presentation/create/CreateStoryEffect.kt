package com.dozy.mobile.feature.story.presentation.create

sealed interface CreateStoryEffect {
    data class Accepted(
        val jobId: String,
    ) : CreateStoryEffect

    data object OpenPlaces : CreateStoryEffect
}
