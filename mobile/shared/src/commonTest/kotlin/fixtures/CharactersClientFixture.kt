package fixtures

import com.dozy.api.client.TechnicalError
import com.dozy.api.client.character.CharactersClients
import com.dozy.api.client.character.v1.CharactersClient
import com.dozy.api.client.character.v1.CharactersCreateResult
import com.dozy.api.client.character.v1.CharactersDeleteResult
import com.dozy.api.client.character.v1.CharactersListResult
import com.dozy.api.client.character.v1.CharactersSaveResult
import com.dozy.api.contracts.v1.character.SaveCharacterRequest

class CharactersClientFixture :
    CharactersClients,
    CharactersClient {
    override val v1: CharactersClient get() = this
    var onList: suspend (String?) -> CharactersListResult = { CharactersListResult.TechnicalError(TechnicalError.Server(503)) }
    var onCreate: suspend (
        SaveCharacterRequest,
    ) -> CharactersCreateResult = { CharactersCreateResult.TechnicalError(TechnicalError.Server(503)) }
    var onSave: suspend (
        String,
        SaveCharacterRequest,
    ) -> CharactersSaveResult = { _, _ -> CharactersSaveResult.TechnicalError(TechnicalError.Server(503)) }
    var onDelete: suspend (String) -> CharactersDeleteResult = { CharactersDeleteResult.TechnicalError(TechnicalError.Server(503)) }

    override suspend fun list(ifNoneMatch: String?) = onList(ifNoneMatch)

    override suspend fun create(
        request: SaveCharacterRequest,
        requestKey: String?,
    ) = onCreate(request)

    override suspend fun save(
        id: String,
        request: SaveCharacterRequest,
    ) = onSave(id, request)

    override suspend fun delete(id: String) = onDelete(id)
}
