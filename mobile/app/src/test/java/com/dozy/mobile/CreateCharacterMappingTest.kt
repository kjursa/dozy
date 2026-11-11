package com.dozy.mobile

import com.dozy.mobile.feature.character.domain.model.*
import com.dozy.mobile.feature.character.domain.model.CreateCharacterErrors
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.SaveStatus
import com.dozy.mobile.feature.character.presentation.create.validateInput
import com.dozy.mobile.feature.character.presentation.create.withValidationErrors
import com.kjursa.foundation.presentation.text.UiText
import org.junit.Assert.*
import org.junit.Test

class CreateCharacterMappingTest {
    @Test fun emptyFormHasNoProductionSampleData() {
        val initial = CreateCharacterUiState()
        assertEquals(UiText.Plain(""), initial.displayName)
        assertEquals(UiText.Plain(""), initial.displayPersonality)
        assertEquals(UiText.Plain(""), initial.displayAppearance)
        assertFalse(initial.canSave)
    }

    @Test fun stableTypeValuesValidateIndependentlyOfDisplayLabels() {
        for (type in Character.Type.entries) {
            val state =
                CreateCharacterUiState(name = "Finn", personality = "Kind", appearance = "Green eyes", selectedType = type.name.lowercase())
            val result = state.validateInput() as CreateCharacterValidation.Valid
            assertEquals(type, result.input.type)
        }
    }

    @Test fun mapsNameErrorsToResourcesAndClearsOtherFields() {
        val cases =
            mapOf(
                CreateCharacterErrors.Name.REQUIRED to UiText.Resource(R.string.character_name_required),
                CreateCharacterErrors.Name.TOO_LONG to UiText.Resource(R.string.character_name_too_long, listOf(50)),
                CreateCharacterErrors.Name.MUST_BE_SINGLE_LINE to UiText.Resource(R.string.character_name_single_line),
            )
        for ((error, text) in cases) {
            val state = CreateCharacterUiState().withValidationErrors(CreateCharacterErrors(name = error))
            assertEquals(text, state.nameError)
            assertNull(state.personalityError)
            assertNull(state.appearanceError)
            assertNull(state.typeError)
        }
    }

    @Test fun mapsDescriptionsAndTypeWithoutSwappingFields() {
        val state =
            CreateCharacterUiState(
                name = "Finn",
                personality = "Curious",
                appearance = "Green eyes",
                selectedType = "Fantasy",
                saveStatus = SaveStatus.FAILURE,
                nameError = UiText.Plain("Old server error"),
            ).withValidationErrors(
                CreateCharacterErrors(
                    personality = CreateCharacterErrors.Description.REQUIRED,
                    appearance = CreateCharacterErrors.Description.TOO_LONG,
                    type = CreateCharacterErrors.Type.REQUIRED,
                ),
            )
        assertEquals("Finn", state.name)
        assertEquals("Curious", state.personality)
        assertEquals("Green eyes", state.appearance)
        assertEquals("Fantasy", state.selectedType)
        assertEquals(SaveStatus.FAILURE, state.saveStatus)
        assertNull(state.nameError)
        assertEquals(UiText.Resource(R.string.character_description_required), state.personalityError)
        assertEquals(UiText.Resource(R.string.character_description_too_long, listOf(2000)), state.appearanceError)
        assertEquals(UiText.Resource(R.string.character_type_required), state.typeError)
        val corrected = state.withErrors()
        assertEquals("Finn", corrected.name)
        assertEquals("Curious", corrected.personality)
        assertEquals("Green eyes", corrected.appearance)
        assertEquals("Fantasy", corrected.selectedType)
        assertEquals(SaveStatus.FAILURE, corrected.saveStatus)
        assertNull(corrected.nameError)
        assertNull(corrected.personalityError)
        assertNull(corrected.appearanceError)
        assertNull(corrected.typeError)
    }
}
