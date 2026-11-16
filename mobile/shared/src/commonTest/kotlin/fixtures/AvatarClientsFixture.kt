package fixtures

import com.dozy.api.client.characteravatar.CharacterAvatarsClients
import com.dozy.api.client.characteravatar.v1.AvatarStartResult
import com.dozy.api.client.characteravatar.v1.AvatarStatusResult
import com.dozy.api.client.characteravatar.v1.CharacterAvatarsClient
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto

fun avatarClients(start: suspend () -> AvatarStartResult): CharacterAvatarsClients =
    object : CharacterAvatarsClients {
        override val v1 =
            object : CharacterAvatarsClient {
                override suspend fun start(request: AvatarRequestDto) = start()

                override suspend fun status(jobId: String): AvatarStatusResult = error("Unexpected status")
            }
    }
