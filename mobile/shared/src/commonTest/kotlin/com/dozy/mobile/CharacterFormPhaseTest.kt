package com.dozy.mobile

import com.dozy.mobile.feature.character.presentation.create.CharacterDraft
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.editableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CharacterFormPhaseTest {
    @Test
    fun actionsFollowValidationAndAvatarAvailability() {
        val empty = CharacterDraft()
        assertIs<CreateCharacterUiState.Editing>(empty.editableState())

        val filled = empty.copy(name = "Finn", personality = "Curious", appearance = "Blue coat")
        val ready = assertIs<CreateCharacterUiState.ReadyToGenerate>(filled.editableState())
        val generating = ready.beginGeneration()
        assertEquals(filled.name, generating.draft.name)

        val withAvatar = filled.copy(avatarId = "asset", avatarUrl = "https://example.test/avatar")
        val avatarReady = assertIs<CreateCharacterUiState.AvatarReady>(withAvatar.editableState())
        val saving = CreateCharacterUiState.Saving(avatarReady)
        assertEquals(avatarReady.avatar, saving.ready.avatar)
    }
}
