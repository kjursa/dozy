package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.flow.first

class StartGeneration(
    private val repository: GenerationRepository,
    private val characters: CharacterRepository,
    private val places: PlaceRepository,
) {
    suspend operator fun invoke(
        input: GenerationInput,
        key: GenerationJob.RequestKey,
    ): Result =
        runCatchingSuspend {
            repository.accepted(key)?.let { return@runCatchingSuspend Result.Accepted(it) }
            repository.configuration(key)?.let { saved ->
                return@runCatchingSuspend repository.start(key, saved)?.let { Result.Accepted(it) } ?: Result.Unavailable
            }
            val available = characters.observe().first()
            val cast = input.cast.map { id -> available.find { it.id == id } ?: return@runCatchingSuspend Result.SelectionUnavailable }
            val place = input.place?.let { places.find(it) ?: return@runCatchingSuspend Result.SelectionUnavailable }
            val config = GenerationConfiguration(cast, input.mode, input.length, place, input.idea, input.ending)
            repository.start(key, config)?.let { Result.Accepted(it) } ?: Result.Unavailable
        }.getOrDefault(Result.Failure)

    sealed interface Result {
        data class Accepted(
            val job: GenerationJob,
        ) : Result

        data object SelectionUnavailable : Result

        data object Unavailable : Result

        data object Failure : Result
    }
}

fun interface GenerationKeyGenerator {
    fun next(): GenerationJob.RequestKey
}
