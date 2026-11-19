package com.dozy.features.characteravatar.domain.model

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.AvatarJobId
import com.dozy.domain.id.OwnerId
import com.dozy.features.character.domain.model.SaveCharacter

class AvatarInput private constructor(val type: String, val personality: String, val appearance: String) {
    sealed interface Result {
        data class Valid(val input: AvatarInput) : Result
        data class Invalid(val field: SaveCharacter.Field, val reason: SaveCharacter.Reason) : Result
    }
    companion object {
        fun parse(type: String, personality: String, appearance: String): Result =
            when (val parsed = SaveCharacter.parse("Avatar", personality, appearance, type, null)) {
                is SaveCharacter.Result.Invalid -> Result.Invalid(parsed.field, parsed.reason)
                is SaveCharacter.Result.Valid -> Result.Valid(AvatarInput(parsed.command.type.name, parsed.command.personality, parsed.command.appearance))
            }
    }
}
enum class AvatarState { QUEUED, RUNNING, READY, FAILED }
data class AvatarJob(val id: AvatarJobId, val owner: OwnerId, val input: AvatarInput, val assetId: AssetId, val url: String, val state: AvatarState)
