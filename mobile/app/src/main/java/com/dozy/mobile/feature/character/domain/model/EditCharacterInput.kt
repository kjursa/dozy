package com.dozy.mobile.feature.character.domain.model

/** An edit combines an existing identity with fields validated by the shared form rules. */
class EditCharacterInput private constructor(
    val id: Character.Id,
    val fields: CreateCharacterInput,
) {
    companion object {
        fun from(
            id: Character.Id,
            fields: CreateCharacterInput,
        ): EditCharacterInput = EditCharacterInput(id, fields)
    }
}
