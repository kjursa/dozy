package com.dozy.mobile.feature.story.presentation.generation

import com.dozy.mobile.R
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.kjursa.foundation.presentation.text.UiText

internal fun GenerationJob.message(): UiText =
    UiText.Resource(
        if (statusUnknown) {
            R.string.generation_unavailable
        } else {
            when (status) {
                GenerationJob.Status.QUEUED -> R.string.story_job_accepted
                GenerationJob.Status.CREATING_TEXT -> R.string.generation_text
                GenerationJob.Status.CREATING_COVER -> R.string.generation_cover
                GenerationJob.Status.READY -> R.string.generation_ready
                GenerationJob.Status.FAILED ->
                    if (failure ==
                        GenerationJob.Failure.CONTENT_REJECTED
                    ) {
                        R.string.generation_rejected
                    } else {
                        R.string.generation_failed
                    }
            }
        },
    )
