package com.dozy.api.contracts.v1.character

import com.dozy.api.contracts.v1.ValidationReason

enum class CharacterField(val value: String, val reasons: Set<ValidationReason>) {
    NAME("name", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG, ValidationReason.SINGLE_LINE)),
    PERSONALITY("personality", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)),
    APPEARANCE("appearance", setOf(ValidationReason.REQUIRED, ValidationReason.TOO_LONG)),
    TYPE("type", setOf(ValidationReason.INVALID_VALUE));

    companion object {
        fun fromValue(value: String): CharacterField? = entries.firstOrNull { it.value == value }
    }
}
