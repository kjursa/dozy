package com.dozy.mobile.feature.place.presentation.form

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceInput
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.launch

class PlaceFormViewModel(
    persistence: StatePersistence<PlaceFormUiState>,
    private val savePlace: SavePlace,
    private val selectPlace: SelectPlace,
    private val editId: String?,
) : MviViewModel<PlaceFormUiState, PlaceFormAction, PlaceFormEffect>(PlaceFormUiState(id = editId), persistence) {
    init {
        initialize()
    }

    private fun initialize() {
        if (uiState.value.initialized || uiState.value.loading) return
        if (editId == null) {
            updateState { copy(initialized = true) }
            return
        }
        updateState { copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = selectPlace(Place.Id(editId))) {
                is SelectPlace.Result.Selected ->
                    if (result.place.canEdit) {
                        val place = result.place
                        updateState {
                            copy(
                                id = editId,
                                initialized = true,
                                loading = false,
                                name = place.name.value,
                                description = place.description.value,
                                inhabitants = place.inhabitants?.value.orEmpty(),
                            )
                        }
                    } else {
                        updateState { copy(loading = false, error = UiText.Resource(Res.string.place_not_editable)) }
                    }
                SelectPlace.Result.Unavailable ->
                    updateState {
                        copy(
                            loading = false,
                            error = UiText.Resource(Res.string.place_unavailable),
                        )
                    }
                SelectPlace.Result.Failure -> updateState { copy(loading = false, error = UiText.Resource(Res.string.place_load_error)) }
            }
        }
    }

    override fun onAction(action: PlaceFormAction) {
        if (uiState.value.saving || uiState.value.navigating) return
        when (action) {
            PlaceFormAction.Back -> {
                updateState { copy(navigating = true) }
                sendEffect(PlaceFormEffect.Back)
            }
            PlaceFormAction.Retry -> initialize()
            is PlaceFormAction.Name -> updateState { copy(name = action.value, nameError = null) }
            is PlaceFormAction.Description -> updateState { copy(description = action.value, descriptionError = null) }
            is PlaceFormAction.Inhabitants -> updateState { copy(inhabitants = action.value, inhabitantsError = null) }
            PlaceFormAction.Save -> save()
        }
    }

    private fun save() {
        val state = uiState.value
        if (!state.initialized) return
        val input =
            when (val validation = PlaceInput.validate(state.name, state.description, state.inhabitants)) {
                is PlaceInput.Validation.Invalid -> {
                    updateState {
                        copy(
                            nameError = validation.errors.name.toUiText(),
                            descriptionError = validation.errors.description.toUiText(),
                            inhabitantsError = validation.errors.inhabitants.toUiText(),
                        )
                    }
                    return
                }
                is PlaceInput.Validation.Valid -> validation.input
            }
        updateState { copy(saving = true, error = null, nameError = null, descriptionError = null, inhabitantsError = null) }
        viewModelScope.launch {
            when (val result = savePlace(input, editId?.let(Place::Id))) {
                is SavePlace.Result.Saved -> sendEffect(PlaceFormEffect.Saved(result.place.id))
                SavePlace.Result.Unavailable -> updateState { copy(saving = false, error = UiText.Resource(Res.string.place_unavailable)) }
                SavePlace.Result.NotEditable -> updateState { copy(saving = false, error = UiText.Resource(Res.string.place_not_editable)) }
                SavePlace.Result.Failure -> updateState { copy(saving = false, error = UiText.Resource(Res.string.place_save_error)) }
            }
        }
    }
}
