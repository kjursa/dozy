package com.dozy.mobile.feature.character.presentation.create

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.core.presentation.CreationDraft
import com.dozy.mobile.core.presentation.newCreationRequestKey
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.dozy.mobile.feature.character.domain.model.EditCharacterInput
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacter
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.coroutines.runCatchingSuspend
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateCharacterViewModel(
    val navigator: AppNavigator,
    statePersistence: StatePersistence<CharacterDraft>,
    private val saveCharacter: SaveCharacter,
    private val generateAvatar: GenerateCharacterAvatar,
    private val observeCharacter: ObserveCharacter,
    private val creationDraft: CreationDraft<CharacterDraft>,
    private val initialId: String? = null,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, Nothing>(
        initialState = CharacterDraft().editableState(),
        statePersistence = CharacterFormPersistence(statePersistence),
    ) {
    init {
        if (initialId != null) loadCharacter() else restoreCreationDraft(CharacterDraft().editableState())
    }

    private fun restoreCreationDraft(emptyForm: CreateCharacterUiState) {
        if (uiState.value != emptyForm) return
        viewModelScope.launch {
            val draft = runCatchingSuspend { creationDraft.load() }.getOrNull()
            if (draft != null && uiState.value == emptyForm) updateState { draft.editableState() }
        }
    }

    private fun loadCharacter() {
        val id = initialId ?: return
        val restoreDraft = uiState.value.draft.editingId == id
        updateState { CreateCharacterUiState.Loading(draft) }
        viewModelScope.launch {
            runCatchingSuspend {
                observeCharacter(Character.Id(id)).first()
            }.onSuccess { character ->
                updateState {
                    when {
                        character == null -> CreateCharacterUiState.Unavailable(draft)
                        restoreDraft -> draft.editableState()
                        else -> draft.withCharacter(character).editableState()
                    }
                }
            }.onFailure {
                updateState { CreateCharacterUiState.LoadFailed(draft, UiText.Resource(Res.string.character_read_failed)) }
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
            navigateTo(CreateCharacterDestination.NavigateBack)
            return
        }
        if (action == CreateCharacterAction.RetryLoadClicked) {
            if (!abandoned && uiState.value is CreateCharacterUiState.LoadFailed) loadCharacter()
            return
        }
        if (abandoned || saved || uiState.value !is CreateCharacterUiState.Editable) return
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
        updateState { if (this is CreateCharacterUiState.Generating) draft.editableState() else this }
    }

    private fun generate() {
        val ready = uiState.value as? CreateCharacterUiState.CanGenerate
        if (ready == null) {
            showValidationErrors()
            return
        }
        val state = ready.draft
        val activeAttempt = ++attempt
        updateState { ready.beginGeneration() }
        generation =
            viewModelScope.launch {
                if (state.editingId == null) runCatchingSuspend { creationDraft.save(state) }
                val result = generateAvatar(ready.input)
                if (abandoned || activeAttempt != attempt) return@launch
                updateState {
                    when (result) {
                        is GenerateCharacterAvatar.Result.Success ->
                            draft.copy(
                                avatarId = result.avatar.id.value,
                                avatarUrl = result.avatar.url.value,
                                avatarDescription = draft.descriptionKey,
                                creationKey = null,
                            ).editableState()
                        GenerateCharacterAvatar.Result.Unavailable ->
                            draft.editableState(UiText.Resource(Res.string.avatar_unavailable))
                        GenerateCharacterAvatar.Result.Failure ->
                            draft.editableState(UiText.Resource(Res.string.avatar_failed))
                    }
                }
            }
    }

    private fun save() {
        val ready = uiState.value as? CreateCharacterUiState.AvatarReady
        if (ready == null) {
            showValidationErrors()
            return
        }
        val state = ready.draft.withErrors()
        val requestKey = if (state.editingId == null) state.creationKey ?: newCreationRequestKey() else null
        val saving = ready.copy(draft = state.copy(creationKey = requestKey), saveFailed = false)
        updateState { CreateCharacterUiState.Saving(saving) }
        viewModelScope.launch {
            if (state.editingId == null && runCatchingSuspend { creationDraft.save(state.copy(creationKey = requestKey)) }.isFailure) {
                updateState { saving.copy(saveFailed = true) }
                return@launch
            }
            val result =
                state.editingId?.let { saveCharacter(EditCharacterInput.from(Character.Id(it), ready.input), ready.avatar) }
                    ?: saveCharacter(ready.input, ready.avatar, requestKey)
            when (result) {
                is SaveCharacter.Result.Success -> {
                    if (state.editingId == null) runCatchingSuspend { creationDraft.clear() }
                    saved = true
                    if (!abandoned) navigateTo(CreateCharacterDestination.Saved(result.saved.character.id))
                }
                SaveCharacter.Result.Unavailable -> updateState { CreateCharacterUiState.Unavailable(saving.draft) }
                SaveCharacter.Result.Failure -> updateState { saving.copy(saveFailed = true) }
            }
        }
    }

    private fun showValidationErrors() {
        val editing = uiState.value as? CreateCharacterUiState.Editing ?: return
        val validation = editing.draft.validateInput() as? CreateCharacterValidation.Invalid ?: return
        updateState { editing.copy(draft = editing.draft.withValidationErrors(validation.errors)) }
    }

    private fun updateCharacterState(reducer: CharacterDraft.() -> CharacterDraft) {
        val edited = uiState.value.draft.reducer()
        val validated =
            when (val validation = edited.validateInput()) {
                is CreateCharacterValidation.Valid -> edited.withErrors()
                is CreateCharacterValidation.Invalid -> edited.withValidationErrors(validation.errors)
            }
        updateState { validated.copy(creationKey = null).editableState() }
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
