package com.dozy.api.client.characteravatar.v1

import com.dozy.api.client.ApiResult
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.characteravatar.AvatarField
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto

interface CharacterAvatarsClient {
    suspend fun start(request: AvatarRequestDto): ApiResult<AvatarJobDto, AvatarStartError>
    suspend fun status(jobId: String): ApiResult<AvatarJobDto, AvatarStatusError>
}
sealed interface AvatarStartError {
    data class Validation(val field: AvatarField, val reason: ValidationReason) : AvatarStartError
    data object Unavailable : AvatarStartError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : AvatarStartError
}
sealed interface AvatarStatusError {
    data object NotFound : AvatarStatusError
    data object Unavailable : AvatarStatusError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : AvatarStatusError
}
