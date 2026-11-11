package com.dozy.mobile.feature.place.domain.usecase

import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.repository.PlaceRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend

/** Only resolves the setting. Inhabitants never become selected main characters. */
class SelectPlace(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(id: Place.Id): Result =
        runCatchingSuspend {
            repository.find(id)?.let { Result.Selected(it) } ?: Result.Unavailable
        }.getOrDefault(Result.Failure)

    sealed interface Result {
        data class Selected(
            val place: Place,
        ) : Result

        data object Unavailable : Result

        data object Failure : Result
    }
}
