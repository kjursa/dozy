package com.dozy.mobile.feature.story.presentation.generation

import com.dozy.mobile.feature.story.domain.model.Story

internal sealed interface GenerationEffect {
    data class Read(
        val id: Story.Id,
    ) : GenerationEffect
}
