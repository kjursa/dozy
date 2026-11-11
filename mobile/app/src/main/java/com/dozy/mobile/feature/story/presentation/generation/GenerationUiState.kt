package com.dozy.mobile.feature.story.presentation.generation

import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.kjursa.foundation.presentation.text.UiText

internal data class GenerationUiState(
    val loading: Boolean = true,
    val jobs: List<GenerationJob> = emptyList(),
    val busy: Boolean = false,
    val error: UiText? = null,
) {
    val homeJob: GenerationJob?
        get() =
            jobs.lastOrNull { it.retryPending || it.status !in setOf(GenerationJob.Status.READY, GenerationJob.Status.FAILED) }
                ?: jobs.lastOrNull()
}
