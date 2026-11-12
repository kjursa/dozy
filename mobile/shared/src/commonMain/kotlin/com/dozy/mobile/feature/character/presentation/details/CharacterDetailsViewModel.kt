package com.dozy.mobile.feature.character.presentation.details

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.DeleteCharacter
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacter
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CharacterDetailsViewModel(
    private val observeCharacter: ObserveCharacter,
    private val deleteCharacter: DeleteCharacter,
    private val id: Character.Id,
) : MviViewModel<CharacterDetailsUiState, CharacterDetailsAction, CharacterDetailsEffect>(CharacterDetailsUiState()) {
    private var observation: Job? = null
    private var leaving = false

    init {
        observe()
    }

    fun resumed() {
        leaving = false
    }

    private fun observe() {
        observation?.cancel()
        updateState { copy(loading = true, readError = null) }
        observation =
            viewModelScope.launch {
                observeCharacter(id)
                    .catch {
                        if (it is CancellationException) throw it
                        updateState { copy(loading = false, readError = UiText.Resource(Res.string.character_read_failed)) }
                    }.collect { character ->
                        updateState { copy(loading = false, character = character, readError = null) }
                    }
            }
    }

    override fun onAction(action: CharacterDetailsAction) {
        val state = uiState.value
        if (state.deleting || leaving) return
        when (action) {
            CharacterDetailsAction.Back -> {
                if (state.deleteDialog) {
                    updateState { copy(deleteDialog = false, deleteError = null) }
                } else {
                    navigate(CharacterDetailsEffect.Back)
                }
            }
            CharacterDetailsAction.Edit -> if (state.character != null && !state.deleteDialog) navigate(CharacterDetailsEffect.Edit(id))
            CharacterDetailsAction.Story -> if (state.character != null && !state.deleteDialog) navigate(CharacterDetailsEffect.Story(id))
            CharacterDetailsAction.Retry -> observe()
            CharacterDetailsAction.Delete -> if (state.character != null) updateState { copy(deleteDialog = true, deleteError = null) }
            CharacterDetailsAction.CancelDelete -> updateState { copy(deleteDialog = false, deleteError = null) }
            CharacterDetailsAction.ConfirmDelete -> {
                if (!state.deleteDialog || state.character == null) return
                updateState { copy(deleting = true, deleteError = null) }
                viewModelScope.launch {
                    when (deleteCharacter(id)) {
                        is DeleteCharacter.Result.Deleted -> navigate(CharacterDetailsEffect.Deleted)
                        DeleteCharacter.Result.Failure ->
                            updateState {
                                copy(deleting = false, deleteError = UiText.Resource(Res.string.character_delete_failed))
                            }
                    }
                }
            }
        }
    }

    private fun navigate(effect: CharacterDetailsEffect) {
        leaving = true
        sendEffect(effect)
    }
}
