package fixtures

import com.dozy.api.client.ApiResult
import com.dozy.api.client.characteravatar.CharacterAvatarsClients
import com.dozy.api.client.characteravatar.v1.AvatarStartError
import com.dozy.api.client.characteravatar.v1.AvatarStatusError
import com.dozy.api.client.characteravatar.v1.CharacterAvatarsClient
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto

fun avatarClients(start: suspend () -> ApiResult<AvatarJobDto, AvatarStartError>): CharacterAvatarsClients =
    object : CharacterAvatarsClients {
        override val v1 =
            object : CharacterAvatarsClient {
                override suspend fun start(request: AvatarRequestDto) = start()

                override suspend fun status(jobId: String): ApiResult<AvatarJobDto, AvatarStatusError> = error("Unexpected status")
            }
    }
