package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.*
import com.dozy.mobile.feature.character.domain.model.CreateCharacterErrors
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.withErrors
import com.kjursa.foundation.presentation.text.UiText
import org.junit.Assert.*
import org.junit.Test

class CreateCharacterMappingTest {
    @Test fun mapsNameErrorsToResourcesAndClearsOtherFields() {
        val cases =
            mapOf(
                CreateCharacterErrors.Name.REQUIRED to UiText.Resource(R.string.character_name_required),
                CreateCharacterErrors.Name.TOO_LONG to UiText.Resource(R.string.character_name_too_long, listOf(50)),
                CreateCharacterErrors.Name.MUST_BE_SINGLE_LINE to UiText.Resource(R.string.character_name_single_line),
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
                    personality = CreateCharacterErrors.Description.REQUIRED,
                    appearance = CreateCharacterErrors.Description.TOO_LONG,
                    type = CreateCharacterErrors.Type.REQUIRED,
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
