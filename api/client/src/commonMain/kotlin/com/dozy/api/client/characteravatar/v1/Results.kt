package com.dozy.api.client.characteravatar.v1

import com.dozy.api.client.TechnicalError as SharedTechnicalError
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarStartError
import com.dozy.api.contracts.v1.characteravatar.AvatarStatusError

sealed interface AvatarStartResult {
    data class Success(val response: AvatarJobDto) : AvatarStartResult
    data class ApiError(val error: AvatarStartError) : AvatarStartResult
    data class TechnicalError(val error: SharedTechnicalError) : AvatarStartResult
}

sealed interface AvatarStatusResult {
    data class Success(val response: AvatarJobDto) : AvatarStatusResult
    data class ApiError(val error: AvatarStatusError) : AvatarStatusResult
    data class TechnicalError(val error: SharedTechnicalError) : AvatarStatusResult
}
