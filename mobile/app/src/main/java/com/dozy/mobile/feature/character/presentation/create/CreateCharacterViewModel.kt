package com.dozy.mobile.feature.character.presentation.create

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.R
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.coroutines.runCatchingSuspend
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class CreateCharacterViewModel(
    statePersistence: StatePersistence<CreateCharacterUiState>,
    private val saveCharacter: SaveCharacter,
    private val generateAvatar: GenerateCharacterAvatar,
    private val observeCharacters: ObserveCharacters,
    private val initialId: String? = null,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, CreateCharacterEffect>(
        initialState = CreateCharacterUiState(),
        statePersistence = statePersistence,
    ) {
    init {
        if (initialId != null && uiState.value.editingId == null) {
            updateState { copy(loading = true) }
            viewModelScope.launch {
                runCatchingSuspend {
                    observeCharacters().first().characters.firstOrNull { it.id.value == initialId }
                }.onSuccess { character ->
                    if (character == null) {
                        updateState { copy(loading = false, unavailable = true) }
                    } else {
                        updateState {
                            copy(
                                loading = false,
                                editingId = character.id.value,
                                name = character.details.name.value,
                                personality = character.details.personality.value,
                                appearance = character.details.appearance.value,
                                selectedType =
                                    character.details.type.name
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                avatarId = character.avatar?.id?.value,
                                avatarUrl = character.avatar?.url?.value,
                            )
                        }
                    }
                }.onFailure { updateState { copy(loading = false, unavailable = true) } }
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
        if (abandoned || saved || uiState.value.busy || uiState.value.unavailable) return
        when (action) {
            CreateCharacterAction.BackClicked -> Unit
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
                            )
                        GenerateCharacterAvatar.Result.Unavailable ->
                            copy(
                                generating = false,
                                generationError = UiText.Resource(R.string.avatar_unavailable),
                            )
                        GenerateCharacterAvatar.Result.Failure ->
                            copy(
                                generating = false,
                                generationError = UiText.Resource(R.string.avatar_failed),
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
            when (val result = saveCharacter(input, avatar, state.editingId?.let { Character.Id(it) })) {
                is SaveCharacter.Result.Success -> {
                    saved = true
                    if (!abandoned) sendEffect(CreateCharacterEffect.Saved(result.saved.character.id))
                }
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
