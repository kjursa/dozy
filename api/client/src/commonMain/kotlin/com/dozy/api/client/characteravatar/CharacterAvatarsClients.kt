package com.dozy.api.client.characteravatar

import com.dozy.api.client.characteravatar.v1.CharacterAvatarsClient

interface CharacterAvatarsClients { val v1: CharacterAvatarsClient }
internal class DefaultCharacterAvatarsClients(override val v1: CharacterAvatarsClient) : CharacterAvatarsClients
