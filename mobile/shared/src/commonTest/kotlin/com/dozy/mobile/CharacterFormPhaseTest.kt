package com.dozy.mobile

import com.dozy.mobile.feature.character.presentation.create.CharacterFormPhase
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.SaveStatus
import com.dozy.mobile.feature.character.presentation.create.formPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CharacterFormPhaseTest {
    @Test
    fun actionsFollowValidationAndAvatarAvailability() {
        val empty = CreateCharacterUiState()
        assertEquals(CharacterFormPhase.Editing, empty.formPhase())

        val filled = empty.copy(name = "Finn", personality = "Curious", appearance = "Blue coat")
        val ready = assertIs<CharacterFormPhase.ReadyToGenerate>(filled.formPhase())
        val generating = ready.beginGeneration(filled)
        assertEquals(CharacterFormPhase.Generating, generating.formPhase())
        assertEquals(filled.name, generating.name)

        val withAvatar = filled.copy(avatarId = "asset", avatarUrl = "https://example.test/avatar")
        assertIs<CharacterFormPhase.AvatarReady>(withAvatar.formPhase())
        assertEquals(CharacterFormPhase.Saving, withAvatar.copy(saveStatus = SaveStatus.SAVING).formPhase())
        assertIs<CharacterFormPhase.AvatarReady>(withAvatar.copy(saveStatus = SaveStatus.FAILURE).formPhase())
    }
}
