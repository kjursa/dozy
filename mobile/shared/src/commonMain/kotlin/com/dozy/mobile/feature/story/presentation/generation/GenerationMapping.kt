package com.dozy.mobile.feature.story.presentation.generation

import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.UiText

fun GenerationJob.message(): UiText =
    UiText.Resource(
        if (statusUnknown) {
            Res.string.generation_unavailable
        } else {
            when (status) {
                GenerationJob.Status.QUEUED -> Res.string.story_job_accepted
                GenerationJob.Status.CREATING_TEXT -> Res.string.generation_text
                GenerationJob.Status.CREATING_COVER -> Res.string.generation_cover
                GenerationJob.Status.READY -> Res.string.generation_ready
                GenerationJob.Status.FAILED ->
                    if (failure ==
                        GenerationJob.Failure.CONTENT_REJECTED
                    ) {
                        Res.string.generation_rejected
                    } else {
                        Res.string.generation_failed
                    }
            }
        },
    )
