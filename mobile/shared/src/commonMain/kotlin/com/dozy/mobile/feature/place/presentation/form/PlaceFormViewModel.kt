package com.dozy.mobile.feature.place.presentation.form

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.core.presentation.CreationDraft
import com.dozy.mobile.core.presentation.newCreationRequestKey
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.model.PlaceInput
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.coroutines.runCatchingSuspend
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.launch

class PlaceFormViewModel(
    val navigator: AppNavigator,
    persistence: StatePersistence<PlaceFormUiState>,
    private val savePlace: SavePlace,
    private val selectPlace: SelectPlace,
    private val creationDraft: CreationDraft<PlaceFormUiState>,
    private val editId: String?,
) : MviViewModel<PlaceFormUiState, PlaceFormAction, Nothing>(PlaceFormUiState(id = editId), persistence) {
    init {
        initialize()
        if (editId == null) restoreCreationDraft(PlaceFormUiState(initialized = true))
    }

    private fun restoreCreationDraft(emptyForm: PlaceFormUiState) {
        if (uiState.value != emptyForm) return
        viewModelScope.launch {
            val draft = runCatchingSuspend { creationDraft.load() }.getOrNull()
            if (draft != null && uiState.value == emptyForm) updateState { draft }
        }
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
                navigateTo(PlaceFormDestination.Back)
            }
            PlaceFormAction.Retry -> initialize()
            is PlaceFormAction.Name -> updateState { copy(name = action.value, nameError = null, creationKey = null) }
            is PlaceFormAction.Description -> updateState { copy(description = action.value, descriptionError = null, creationKey = null) }
            is PlaceFormAction.Inhabitants -> updateState { copy(inhabitants = action.value, inhabitantsError = null, creationKey = null) }
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
        val requestKey = if (editId == null) state.creationKey ?: newCreationRequestKey() else null
        updateState {
            copy(saving = true, error = null, nameError = null, descriptionError = null, inhabitantsError = null, creationKey = requestKey)
        }
        viewModelScope.launch {
            if (editId == null && runCatchingSuspend { creationDraft.save(state.copy(creationKey = requestKey)) }.isFailure) {
                updateState { copy(saving = false, error = UiText.Resource(Res.string.place_save_error)) }
                return@launch
            }
            when (val result = savePlace(input, editId?.let(Place::Id), requestKey)) {
                is SavePlace.Result.Saved -> {
                    if (editId == null) runCatchingSuspend { creationDraft.clear() }
                    navigateTo(PlaceFormDestination.Saved(result.place.id))
                }
                SavePlace.Result.Unavailable -> updateState { copy(saving = false, error = UiText.Resource(Res.string.place_unavailable)) }
                SavePlace.Result.NotEditable -> updateState { copy(saving = false, error = UiText.Resource(Res.string.place_not_editable)) }
                SavePlace.Result.Failure -> updateState { copy(saving = false, error = UiText.Resource(Res.string.place_save_error)) }
            }
        }
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
