package com.dozy.mobile.feature.story.presentation.create

import com.dozy.mobile.feature.story.domain.model.GenerationErrors
import com.dozy.mobile.feature.story.domain.model.GenerationInput
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText

fun CreateStoryUiState.validateInput() =
    GenerationInput.from(
        selectedHeroes,
        automaticPlot,
        selectedLength,
        placeId,
        specialRequest,
        ending,
    )

fun CreateStoryUiState.withErrors(errors: GenerationErrors) =
    copy(
        castError = errors.cast?.let { UiText.Resource(Res.string.story_cast_required) },
        lengthError = errors.length?.let { UiText.Resource(Res.string.story_length_invalid) },
        ideaError = errors.idea?.let { UiText.Resource(Res.string.story_idea_too_long) },
        endingError = errors.ending?.let { UiText.Resource(Res.string.story_ending_too_long) },
    )
