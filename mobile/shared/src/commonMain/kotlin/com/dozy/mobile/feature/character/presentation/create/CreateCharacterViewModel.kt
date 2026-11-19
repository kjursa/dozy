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
    statePersistence: StatePersistence<CreateCharacterUiState>,
    private val saveCharacter: SaveCharacter,
    private val generateAvatar: GenerateCharacterAvatar,
    private val observeCharacter: ObserveCharacter,
    private val creationDraft: CreationDraft<CreateCharacterUiState>,
    private val initialId: String? = null,
) : MviViewModel<CreateCharacterUiState, CreateCharacterAction, Nothing>(
        initialState = CreateCharacterUiState(),
        statePersistence = statePersistence,
    ) {
    init {
        if (initialId != null) loadCharacter() else restoreCreationDraft(CreateCharacterUiState())
    }

    private fun restoreCreationDraft(emptyForm: CreateCharacterUiState) {
        if (uiState.value != emptyForm) return
        viewModelScope.launch {
            val draft = runCatchingSuspend { creationDraft.load() }.getOrNull()
            if (draft != null && uiState.value == emptyForm) updateState { draft }
        }
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
            navigateTo(CreateCharacterDestination.NavigateBack)
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
        val state = uiState.value
        val activeAttempt = ++attempt
        updateState { copy(generating = true, generationError = null) }
        generation =
            viewModelScope.launch {
                if (state.editingId == null) runCatchingSuspend { creationDraft.save(state) }
                val result = generateAvatar(input)
                if (abandoned || activeAttempt != attempt) return@launch
                updateState {
                    when (result) {
                        is GenerateCharacterAvatar.Result.Success ->
                            copy(
                                generating = false,
                                avatarId = result.avatar.id.value,
                                avatarUrl = result.avatar.url.value,
                                avatarDescription = descriptionKey,
                                creationKey = null,
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
        val requestKey = if (state.editingId == null) state.creationKey ?: newCreationRequestKey() else null
        updateState { copy(saveStatus = SaveStatus.SAVING, creationKey = requestKey) }
        viewModelScope.launch {
            if (state.editingId == null && runCatchingSuspend { creationDraft.save(state.copy(creationKey = requestKey)) }.isFailure) {
                updateState { copy(saveStatus = SaveStatus.FAILURE) }
                return@launch
            }
            val result =
                state.editingId?.let { saveCharacter(EditCharacterInput.from(Character.Id(it), input), avatar) }
                    ?: saveCharacter(input, avatar, requestKey)
            when (result) {
                is SaveCharacter.Result.Success -> {
                    if (state.editingId == null) runCatchingSuspend { creationDraft.clear() }
                    saved = true
                    if (!abandoned) navigateTo(CreateCharacterDestination.Saved(result.saved.character.id))
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
        updateState { validated.copy(saveStatus = SaveStatus.EDITING, generationError = null, creationKey = null) }
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
