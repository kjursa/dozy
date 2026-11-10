package com.dozy.mobile.feature.character.domain

sealed interface CreateCharacterValidation {
    data class Valid(
        val input: CreateCharacterInput,
    ) : CreateCharacterValidation

    data class Invalid(
        val errors: CreateCharacterErrors,
    ) : CreateCharacterValidation
}
