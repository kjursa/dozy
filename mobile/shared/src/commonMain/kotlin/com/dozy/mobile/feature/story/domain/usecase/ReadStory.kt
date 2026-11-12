package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class ReadStory(
    private val repository: GenerationRepository,
) {
    suspend operator fun invoke(id: Story.Id): Result =
        runCatchingSuspend {
            require(id.value.matches(Regex("[A-Za-z0-9_-]{1,128}")))
            repository.readStory(id)?.let { Result.Found(it) } ?: Result.Missing
        }.getOrElse { Result.Unavailable }

    sealed interface Result {
        data class Found(
            val story: Story,
        ) : Result

        data object Missing : Result

        data object Unavailable : Result
    }
}
