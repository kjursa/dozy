package com.dozy.mobile.feature.place.domain.usecase

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceInput
import com.dozy.mobile.feature.place.domain.model.PlaceSync
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

class SavePlace(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(
        input: PlaceInput,
        id: Place.Id?,
        requestKey: String? = null,
    ): Result =
        runCatchingSuspend {
            if (id == null) return@runCatchingSuspend Result.Saved(repository.create(input, requestKey), PlaceSync.SYNCED)
            val existing = repository.find(id) ?: return@runCatchingSuspend Result.Unavailable
            if (!existing.canEdit) return@runCatchingSuspend Result.NotEditable
            val place = existing.copy(name = input.name, description = input.description, inhabitants = input.inhabitants)
            val sync = repository.save(place)
            Result.Saved(place, sync)
        }.getOrDefault(Result.Failure)

    sealed interface Result {
        data class Saved(
            val place: Place,
            val sync: PlaceSync,
        ) : Result

        data object Unavailable : Result

        data object NotEditable : Result

        data object Failure : Result
    }
}
