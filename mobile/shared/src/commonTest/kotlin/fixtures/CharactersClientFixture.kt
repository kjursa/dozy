package fixtures

import com.dozy.api.client.ApiResult
import com.dozy.api.client.character.CharactersClients
import com.dozy.api.client.character.v1.CharactersClient
import com.dozy.api.client.character.v1.CharactersDeleteError
import com.dozy.api.client.character.v1.CharactersListError
import com.dozy.api.client.character.v1.CharactersSaveError
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.SaveCharacterRequest

class CharactersClientFixture :
    CharactersClients,
    CharactersClient {
    override val v1: CharactersClient get() = this
    var onList: suspend () -> ApiResult<List<CharacterDto>, CharactersListError> = { ApiResult.Failure(CharactersListError.Unavailable) }
    var onSave: suspend (
        String,
        SaveCharacterRequest,
    ) -> ApiResult<Unit, CharactersSaveError> = { _, _ -> ApiResult.Failure(CharactersSaveError.Unavailable) }
    var onDelete: suspend (String) -> ApiResult<Unit, CharactersDeleteError> = { ApiResult.Failure(CharactersDeleteError.Unavailable) }

    override suspend fun list() = onList()

    override suspend fun save(
        id: String,
        request: SaveCharacterRequest,
    ) = onSave(id, request)

    override suspend fun delete(id: String) = onDelete(id)
}
