package com.dozy.mobile.feature.character.domain

/** Only validated creation requests may reach the save use case. Existing records use Character.Details. */
class CreateCharacterInput private constructor(
    val name: Character.Name,
    val personality: Character.Personality,
    val appearance: Character.Appearance,
    val type: Character.Type,
) {
    companion object {
        const val NAME_MAX_LENGTH = 50
        const val DESCRIPTION_MAX_LENGTH = 2000

        fun from(
            name: String,
            personality: String,
            appearance: String,
            type: Character.Type?,
        ): CreateCharacterValidation {
            val normalizedName = name.trim()
            val normalizedPersonality = personality.trim()
            val normalizedAppearance = appearance.trim()
            val errors =
                CreateCharacterErrors(
                    name =
                        when {
                            normalizedName.isEmpty() -> CharacterNameError.REQUIRED
                            normalizedName.length > NAME_MAX_LENGTH -> CharacterNameError.TOO_LONG
                            normalizedName.any { it == '\n' || it == '\r' } -> CharacterNameError.MUST_BE_SINGLE_LINE
                            else -> null
                        },
                    personality = validateDescription(normalizedPersonality),
                    appearance = validateDescription(normalizedAppearance),
                    type = if (type == null) CharacterTypeError.REQUIRED else null,
                )
            if (!errors.isEmpty) return CreateCharacterValidation.Invalid(errors)
            return CreateCharacterValidation.Valid(
                CreateCharacterInput(
                    Character.Name(normalizedName),
                    Character.Personality(normalizedPersonality),
                    Character.Appearance(normalizedAppearance),
                    requireNotNull(type),
                ),
            )
        }

        private fun validateDescription(value: String): CharacterDescriptionError? =
            when {
                value.isEmpty() -> CharacterDescriptionError.REQUIRED
                value.length > DESCRIPTION_MAX_LENGTH -> CharacterDescriptionError.TOO_LONG
                else -> null
            }
    }
}
