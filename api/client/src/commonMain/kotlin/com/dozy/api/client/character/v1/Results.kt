package com.dozy.api.client.character.v1

import com.dozy.api.client.TechnicalError as SharedTechnicalError
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharactersDeleteError
import com.dozy.api.contracts.v1.character.CharactersListError
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.api.contracts.v1.character.CreatedCharacterResponse

sealed interface CharactersListResult {
    data class Success(
        val response: List<CharacterDto>,
        val etag: String? = null,
    ) : CharactersListResult

    data object NotModified : CharactersListResult
    data class ApiError(val error: CharactersListError) : CharactersListResult
    data class TechnicalError(val error: SharedTechnicalError) : CharactersListResult
}

sealed interface CharactersCreateResult {
    data class Success(val response: CreatedCharacterResponse) : CharactersCreateResult
    data class ApiError(val error: CharactersSaveError) : CharactersCreateResult
    data class TechnicalError(val error: SharedTechnicalError) : CharactersCreateResult
}

sealed interface CharactersSaveResult {
    data object Success : CharactersSaveResult
    data class ApiError(val error: CharactersSaveError) : CharactersSaveResult
    data class TechnicalError(val error: SharedTechnicalError) : CharactersSaveResult
}

sealed interface CharactersDeleteResult {
    data object Success : CharactersDeleteResult
    data class ApiError(val error: CharactersDeleteError) : CharactersDeleteResult
    data class TechnicalError(val error: SharedTechnicalError) : CharactersDeleteResult
}
