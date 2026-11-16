package com.dozy.api.contracts

import com.dozy.api.contracts.v1.ApiBusinessError
import com.dozy.api.contracts.v1.character.CharactersDeleteError
import com.dozy.api.contracts.v1.character.CharactersListError
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.api.contracts.v1.characteravatar.AvatarStartError
import com.dozy.api.contracts.v1.characteravatar.AvatarStatusError
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusError
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryReadError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class SharedBusinessErrorsTest {
    @Test fun `all business errors round trip using operation and backend serializers`() {
        for (error in CharactersListError.known + CharactersListError.Unknown("future.code")) {
            val body = Json.encodeToString<CharactersListError>(error)
            assertEquals(error, Json.decodeFromString<CharactersListError>(body))
            assertEquals(error, Json.decodeFromString<CharactersListError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in CharactersSaveError.known + CharactersSaveError.Unknown("future.code")) {
            val body = Json.encodeToString<CharactersSaveError>(error)
            assertEquals(error, Json.decodeFromString<CharactersSaveError>(body))
            assertEquals(error, Json.decodeFromString<CharactersSaveError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in CharactersDeleteError.known + CharactersDeleteError.Unknown("future.code")) {
            val body = Json.encodeToString<CharactersDeleteError>(error)
            assertEquals(error, Json.decodeFromString<CharactersDeleteError>(body))
            assertEquals(error, Json.decodeFromString<CharactersDeleteError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in AvatarStartError.known + AvatarStartError.Unknown("future.code")) {
            val body = Json.encodeToString<AvatarStartError>(error)
            assertEquals(error, Json.decodeFromString<AvatarStartError>(body))
            assertEquals(error, Json.decodeFromString<AvatarStartError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in AvatarStatusError.known + AvatarStatusError.Unknown("future.code")) {
            val body = Json.encodeToString<AvatarStatusError>(error)
            assertEquals(error, Json.decodeFromString<AvatarStatusError>(body))
            assertEquals(error, Json.decodeFromString<AvatarStatusError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in GenerationStartError.known + GenerationStartError.Unknown("future.code")) {
            val body = Json.encodeToString<GenerationStartError>(error)
            assertEquals(error, Json.decodeFromString<GenerationStartError>(body))
            assertEquals(error, Json.decodeFromString<GenerationStartError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in GenerationStatusError.known + GenerationStatusError.Unknown("future.code")) {
            val body = Json.encodeToString<GenerationStatusError>(error)
            assertEquals(error, Json.decodeFromString<GenerationStatusError>(body))
            assertEquals(error, Json.decodeFromString<GenerationStatusError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in GenerationRetryError.known + GenerationRetryError.Unknown("future.code")) {
            val body = Json.encodeToString<GenerationRetryError>(error)
            assertEquals(error, Json.decodeFromString<GenerationRetryError>(body))
            assertEquals(error, Json.decodeFromString<GenerationRetryError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in StoryListError.known + StoryListError.Unknown("future.code")) {
            val body = Json.encodeToString<StoryListError>(error)
            assertEquals(error, Json.decodeFromString<StoryListError>(body))
            assertEquals(error, Json.decodeFromString<StoryListError>(Json.encodeToString<ApiBusinessError>(error)))
        }
        for (error in StoryReadError.known + StoryReadError.Unknown("future.code")) {
            val body = Json.encodeToString<StoryReadError>(error)
            assertEquals(error, Json.decodeFromString<StoryReadError>(body))
            assertEquals(error, Json.decodeFromString<StoryReadError>(Json.encodeToString<ApiBusinessError>(error)))
        }
    }
}
