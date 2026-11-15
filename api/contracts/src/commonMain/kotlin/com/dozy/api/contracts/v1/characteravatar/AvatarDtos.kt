package com.dozy.api.contracts.v1.characteravatar

import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterField
import com.dozy.api.contracts.v1.ValidationReason
import kotlinx.serialization.Serializable

@Serializable
data class AvatarRequestDto(val type: String, val personality: String, val appearance: String)

@Serializable
data class AvatarJobDto(
    val jobId: String,
    val status: String,
    val asset: AvatarDto? = null,
    val error: String? = null,
)

enum class AvatarJobState { QUEUED, RUNNING, READY, FAILED }
enum class AvatarFailureCode { GENERATION_FAILED }

/** Avatar input shares character type and description rules, excluding name. */
enum class AvatarField(private val characterField: CharacterField) {
    TYPE(CharacterField.TYPE), PERSONALITY(CharacterField.PERSONALITY), APPEARANCE(CharacterField.APPEARANCE);
    val value: String get() = characterField.value
    val reasons: Set<ValidationReason> get() = characterField.reasons
    companion object {
        fun fromValue(value: String): AvatarField? = entries.firstOrNull { it.value == value }
    }
}
