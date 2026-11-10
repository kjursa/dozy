package com.dozy.mobile.domain.character

sealed interface CreateCharacterValidation {
    data class Valid(
        val input: CreateCharacterInput,
    ) : CreateCharacterValidation

    data class Invalid(
        val errors: CreateCharacterErrors,
    ) : CreateCharacterValidation
}
