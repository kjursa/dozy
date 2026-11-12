package com.dozy.mobile.feature.story.presentation.generation

import com.dozy.mobile.feature.story.domain.model.GenerationJob

sealed interface GenerationAction {
    data class Retry(
        val id: GenerationJob.Id,
    ) : GenerationAction

    data class Refresh(
        val id: GenerationJob.Id,
    ) : GenerationAction

    data class Open(
        val id: GenerationJob.Id,
    ) : GenerationAction
}
