package com.dozy.mobile.feature.place.presentation.list

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.R
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class PlacesViewModel(
    private val observePlaces: ObservePlaces,
    private val refreshPlaces: RefreshPlaces,
    private val selectPlace: SelectPlace,
) : MviViewModel<PlacesUiState, PlacesAction, PlacesEffect>(PlacesUiState()) {
    private var observation: Job? = null
    private var refresh: Job? = null

    init {
        load()
    }

    private fun load() {
        if (refresh?.isActive == true) return
        updateState { copy(loading = true, error = null) }
        if (observation?.isActive != true) {
            observation =
                viewModelScope.launch {
                    observePlaces()
                        .catch { updateState { copy(loading = false, error = UiText.Resource(R.string.places_load_error)) } }
                        .collect { updateState { copy(places = it) } }
                }
        }
        refresh =
            viewModelScope.launch {
                val result = refreshPlaces()
                updateState {
                    copy(
                        loading = false,
                        error =
                            if (result ==
                                RefreshPlaces.Result.Failure
                            ) {
                                UiText.Resource(R.string.places_load_error)
                            } else {
                                error
                            },
                    )
                }
            }
    }

    override fun onAction(action: PlacesAction) {
        if (uiState.value.navigating) return
        when (action) {
            PlacesAction.Retry -> load()
            PlacesAction.Back -> navigate(PlacesEffect.Back)
            PlacesAction.Add -> navigate(PlacesEffect.Form(null))
            PlacesAction.Surprise -> navigate(PlacesEffect.Selected(null))
            is PlacesAction.Edit ->
                if (uiState.value.places.any { it.id == action.id && it.canEdit }) {
                    navigate(
                        PlacesEffect.Form(action.id),
                    )
                }
            is PlacesAction.Select -> {
                updateState { copy(navigating = true) }
                viewModelScope.launch {
                    when (val result = selectPlace(action.id)) {
                        is SelectPlace.Result.Selected -> sendEffect(PlacesEffect.Selected(result.place.id))
                        SelectPlace.Result.Unavailable ->
                            updateState {
                                copy(
                                    navigating = false,
                                    error = UiText.Resource(R.string.places_load_error),
                                )
                            }
                    }
                }
            }
        }
    }

    private fun navigate(effect: PlacesEffect) {
        updateState { copy(navigating = true) }
        sendEffect(effect)
    }

    fun navigationHandled() {
        updateState { copy(navigating = false) }
    }
}
