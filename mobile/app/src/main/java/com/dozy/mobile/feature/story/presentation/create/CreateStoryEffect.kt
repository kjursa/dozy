package com.dozy.mobile.feature.story.presentation.create

internal sealed interface CreateStoryEffect {
    data object OpenPlaces : CreateStoryEffect
}
