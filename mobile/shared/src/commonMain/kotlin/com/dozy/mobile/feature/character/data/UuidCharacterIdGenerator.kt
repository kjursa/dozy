package com.dozy.mobile.feature.character.data

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.CharacterIdGenerator
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UuidCharacterIdGenerator : CharacterIdGenerator {
    override fun next() = Character.Id(Uuid.generateV7().toString())
}
