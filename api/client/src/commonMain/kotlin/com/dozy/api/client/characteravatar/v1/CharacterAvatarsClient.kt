package com.dozy.api.client.characteravatar.v1

import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto

interface CharacterAvatarsClient {
    suspend fun start(request: AvatarRequestDto): AvatarStartResult
    suspend fun status(jobId: String): AvatarStatusResult
}
