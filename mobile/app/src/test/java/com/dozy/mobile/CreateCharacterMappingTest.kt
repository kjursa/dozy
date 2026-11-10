package com.dozy.mobile

import com.dozy.mobile.core.presentation.text.UiText
import com.dozy.mobile.feature.character.domain.*
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.withErrors
import org.junit.Assert.*
import org.junit.Test

class CreateCharacterMappingTest {
    @Test fun mapsNameErrorsToResourcesAndClearsOtherFields() {
        val cases =
            mapOf(
                CharacterNameError.REQUIRED to UiText.Resource(R.string.character_name_required),
                CharacterNameError.TOO_LONG to UiText.Resource(R.string.character_name_too_long, listOf(50)),
                CharacterNameError.MUST_BE_SINGLE_LINE to UiText.Resource(R.string.character_name_single_line),
            )
        for ((error, text) in cases) {
            val state = CreateCharacterUiState().withErrors(CreateCharacterErrors(name = error))
            assertEquals(text, state.nameError)
            assertNull(state.personalityError)
            assertNull(state.appearanceError)
            assertNull(state.typeError)
        }
    }

    @Test fun mapsDescriptionsAndTypeWithoutSwappingFields() {
        val state =
            CreateCharacterUiState().withErrors(
                CreateCharacterErrors(
                    personality = CharacterDescriptionError.REQUIRED,
                    appearance = CharacterDescriptionError.TOO_LONG,
                    type = CharacterTypeError.REQUIRED,
                ),
            )
        assertNull(state.nameError)
        assertEquals(UiText.Resource(R.string.character_description_required), state.personalityError)
        assertEquals(UiText.Resource(R.string.character_description_too_long, listOf(2000)), state.appearanceError)
        assertEquals(UiText.Resource(R.string.character_type_required), state.typeError)
        val corrected = state.withErrors()
        assertNull(corrected.nameError)
        assertNull(corrected.personalityError)
        assertNull(corrected.appearanceError)
        assertNull(corrected.typeError)
    }
}
