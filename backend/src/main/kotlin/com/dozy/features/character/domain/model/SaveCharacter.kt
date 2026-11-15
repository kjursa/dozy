package com.dozy.features.character.domain.model

class SaveCharacter private constructor(
    val name: String,
    val personality: String,
    val appearance: String,
    val type: Character.Type,
    val avatar: Character.Avatar?,
) {
    enum class Field { NAME, PERSONALITY, APPEARANCE, TYPE }
    enum class Reason { REQUIRED, TOO_LONG, SINGLE_LINE, INVALID_VALUE }
    sealed interface Result {
        data class Valid(val command: SaveCharacter) : Result
        data class Invalid(val field: Field, val reason: Reason) : Result
    }

    companion object {
        fun parse(name: String, personality: String, appearance: String, type: String, avatar: Character.Avatar?): Result {
            val normalizedName = name.trim()
            val normalizedPersonality = personality.trim()
            val normalizedAppearance = appearance.trim()
            for ((field, value, limit) in listOf(
                Triple(Field.NAME, normalizedName, 50),
                Triple(Field.PERSONALITY, normalizedPersonality, 2000),
                Triple(Field.APPEARANCE, normalizedAppearance, 2000),
            )) {
                if (value.isEmpty()) return Result.Invalid(field, Reason.REQUIRED)
                if (value.length > limit) return Result.Invalid(field, Reason.TOO_LONG)
                if (field == Field.NAME && value.any { it in "\r\n\u0085\u2028\u2029" }) {
                    return Result.Invalid(field, Reason.SINGLE_LINE)
                }
            }
            val parsedType = Character.Type.entries.firstOrNull { it.name == type }
                ?: return Result.Invalid(Field.TYPE, Reason.INVALID_VALUE)
            return Result.Valid(SaveCharacter(normalizedName, normalizedPersonality, normalizedAppearance, parsedType, avatar))
        }
    }
}
