package com.dozy.mobile.feature.place.domain.usecase

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceInput
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

fun interface PlaceIdGenerator {
    fun generate(): Place.Id
}

class SavePlace(
    private val repository: PlaceRepository,
    private val ids: PlaceIdGenerator,
) {
    suspend operator fun invoke(
        input: PlaceInput,
        id: Place.Id?,
    ): Result =
        runCatchingSuspend {
            if (id != null && repository.find(id)?.canEdit != true) return@runCatchingSuspend Result.NotEditable
            val place = Place(id ?: ids.generate(), input.name, input.description, input.inhabitants, Place.Ownership.CUSTOM, true)
            val sync = repository.save(place)
            Result.Saved(place, sync)
        }.getOrDefault(Result.Failure)

    sealed interface Result {
        data class Saved(
            val place: Place,
            val sync: PlaceSync,
        ) : Result

        data object NotEditable : Result

        data object Failure : Result
    }
}
