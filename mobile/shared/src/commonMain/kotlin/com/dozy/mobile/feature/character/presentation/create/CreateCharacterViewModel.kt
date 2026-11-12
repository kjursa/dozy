package com.dozy.mobile.feature.character.presentation.create

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.model.EditCharacterInput
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacter
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.coroutines.runCatchingSuspend
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateCharacterViewModel(
    statePersistence: StatePersistence<CreateCharacterUiState>,
    private val saveCharacter: SaveCharacter,
    private val generateAvatar: GenerateCharacterAvatar,
    private val observeCharacter: ObserveCharacter,
    private val initialId: String? = null,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, CreateCharacterEffect>(
        initialState = CreateCharacterUiState(),
        statePersistence = statePersistence,
    ) {
    init {
        if (initialId != null) loadCharacter()
    }

    private fun loadCharacter() {
        val id = initialId ?: return
        val restoreDraft = uiState.value.editingId == id
        updateState { copy(loading = true, loadError = null, unavailable = false) }
        viewModelScope.launch {
            runCatchingSuspend {
                observeCharacter(Character.Id(id)).first()
            }.onSuccess { character ->
                updateState {
                    when {
                        character == null -> copy(loading = false, unavailable = true)
                        restoreDraft -> copy(loading = false)
                        else -> withCharacter(character)
                    }
                }
            }.onFailure {
                updateState { copy(loading = false, loadError = UiText.Resource(Res.string.character_read_failed)) }
            }
        }
    }

    private var attempt = 0L
    private var generation: Job? = null
    private var abandoned = false
    private var saved = false

    override fun onAction(action: CreateCharacterAction) {
        if (action == CreateCharacterAction.BackClicked) {
            abandon()
            sendEffect(CreateCharacterEffect.NavigateBack)
            return
        }
        if (action == CreateCharacterAction.RetryLoadClicked) {
            if (!abandoned && !uiState.value.busy && uiState.value.loadError != null) loadCharacter()
            return
        }
        if (abandoned || saved || uiState.value.busy || uiState.value.unavailable || uiState.value.loadError != null) return
        when (action) {
            CreateCharacterAction.BackClicked, CreateCharacterAction.RetryLoadClicked -> Unit
            CreateCharacterAction.SaveClicked -> save()
            CreateCharacterAction.PreviewClicked -> generate()
            is CreateCharacterAction.TypeSelected -> updateCharacterState { withType(action.type) }
            is CreateCharacterAction.NameChanged -> updateCharacterState { withName(action.name) }
            is CreateCharacterAction.PersonalityChanged -> updateCharacterState { withPersonality(action.personality) }
            is CreateCharacterAction.AppearanceChanged -> updateCharacterState { withAppearance(action.appearance) }
        }
    }

    /** Reattaching after configuration changes starts no network operation. */
    fun resume() {
        abandoned = false
    }

    /** Stop observing attempts when navigation or configuration detaches the form. */
    fun abandon() {
        abandoned = true
        attempt++
        generation?.cancel()
        updateState { copy(generating = false) }
    }

    private fun validatedInput() =
        when (val validation = uiState.value.validateInput()) {
            is CreateCharacterValidation.Valid -> validation.input.also { updateState { withErrors() } }
            is CreateCharacterValidation.Invalid -> {
                updateState { withValidationErrors(validation.errors) }
                null
            }
        }

    private fun generate() {
        val input = validatedInput() ?: return
        val activeAttempt = ++attempt
        updateState { copy(generating = true, generationError = null) }
        generation =
            viewModelScope.launch {
                val result = generateAvatar(input)
                if (abandoned || activeAttempt != attempt) return@launch
                updateState {
                    when (result) {
                        is GenerateCharacterAvatar.Result.Success ->
                            copy(
                                generating = false,
                                avatarId = result.avatar.id.value,
                                avatarUrl = result.avatar.url.value,
                                avatarDescription = uiState.value.descriptionKey,
                            )
                        GenerateCharacterAvatar.Result.Unavailable ->
                            copy(
                                generating = false,
                                generationError = UiText.Resource(Res.string.avatar_unavailable),
                            )
                        GenerateCharacterAvatar.Result.Failure ->
                            copy(
                                generating = false,
                                generationError = UiText.Resource(Res.string.avatar_failed),
                            )
                    }
                }
            }
    }

    private fun save() {
        val input = validatedInput() ?: return
        val state = uiState.value
        val avatar =
            Character.Avatar(
                Character.Avatar.Id(state.avatarId ?: return),
                Character.Avatar.Url(state.avatarUrl ?: return),
            )
        updateState { copy(saveStatus = SaveStatus.SAVING) }
        viewModelScope.launch {
            val result =
                state.editingId?.let { saveCharacter(EditCharacterInput.from(Character.Id(it), input), avatar) }
                    ?: saveCharacter(input, avatar)
            when (result) {
                is SaveCharacter.Result.Success -> {
                    saved = true
                    if (!abandoned) sendEffect(CreateCharacterEffect.Saved(result.saved.character.id))
                }
                SaveCharacter.Result.Unavailable -> updateState { copy(saveStatus = SaveStatus.EDITING, unavailable = true) }
                SaveCharacter.Result.Failure -> updateState { copy(saveStatus = SaveStatus.FAILURE) }
            }
        }
    }

    private fun updateCharacterState(reducer: CreateCharacterUiState.() -> CreateCharacterUiState) {
        val edited = uiState.value.reducer()
        val validated =
            when (val validation = edited.validateInput()) {
                is CreateCharacterValidation.Valid -> edited.withErrors()
                is CreateCharacterValidation.Invalid -> edited.withValidationErrors(validation.errors)
            }
        updateState { validated.copy(saveStatus = SaveStatus.EDITING, generationError = null) }
    }
}
