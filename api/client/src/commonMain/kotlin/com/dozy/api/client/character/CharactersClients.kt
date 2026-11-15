package com.dozy.api.client.character

import com.dozy.api.client.character.v1.CharactersClient as CharactersClientV1

interface CharactersClients {
    val v1: CharactersClientV1
}

internal class DefaultCharactersClients(
    override val v1: CharactersClientV1,
) : CharactersClients
