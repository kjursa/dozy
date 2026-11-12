package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.story.domain.repository.GenerationRepository

class ObserveGenerations(
    private val repository: GenerationRepository,
) {
    operator fun invoke() = repository.observe()
}
