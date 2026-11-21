package com.dozy.mobile.feature.character.presentation.create

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CreateCharacterInput
import com.dozy.mobile.feature.character.domain.model.CreateCharacterValidation
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.presentation.text.UiText

sealed interface CreateCharacterUiState {
    val draft: CharacterDraft

    data class Loading(override val draft: CharacterDraft) : CreateCharacterUiState

    data class Unavailable(override val draft: CharacterDraft) : CreateCharacterUiState

    data class LoadFailed(override val draft: CharacterDraft, val error: UiText) : CreateCharacterUiState

    sealed interface Editable : CreateCharacterUiState {
        val generationError: UiText?
    }

    data class Editing(
        override val draft: CharacterDraft,
        override val generationError: UiText? = null,
    ) : Editable

    sealed interface CanGenerate : Editable {
        val input: CreateCharacterInput

        fun beginGeneration() = Generating(draft.withErrors())
    }

    data class ReadyToGenerate(
        override val draft: CharacterDraft,
        override val input: CreateCharacterInput,
        override val generationError: UiText? = null,
    ) : CanGenerate

    data class AvatarReady(
        override val draft: CharacterDraft,
        override val input: CreateCharacterInput,
        val avatar: Character.Avatar,
        override val generationError: UiText? = null,
        val saveFailed: Boolean = false,
    ) : CanGenerate

    data class Generating(override val draft: CharacterDraft) : CreateCharacterUiState

    data class Saving(val ready: AvatarReady) : CreateCharacterUiState {
        override val draft get() = ready.draft
    }
}

internal fun CharacterDraft.editableState(generationError: UiText? = null): CreateCharacterUiState.Editable {
    val input = (validateInput() as? CreateCharacterValidation.Valid)?.input
        ?: return CreateCharacterUiState.Editing(this, generationError)
    val id = avatarId ?: return CreateCharacterUiState.ReadyToGenerate(this, input, generationError)
    val url = avatarUrl ?: return CreateCharacterUiState.ReadyToGenerate(this, input, generationError)
    val avatar = Character.Avatar(Character.Avatar.Id(id), Character.Avatar.Url(url))
    return CreateCharacterUiState.AvatarReady(this, input, avatar, generationError)
}

internal class CharacterFormPersistence(
    private val persistence: StatePersistence<CharacterDraft>,
) : StatePersistence<CreateCharacterUiState> {
    override fun restore(defaultState: CreateCharacterUiState): CreateCharacterUiState =
        persistence.restore(defaultState.draft).editableState()

    override fun save(state: CreateCharacterUiState) = persistence.save(state.draft)
}
