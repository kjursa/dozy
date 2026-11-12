package com.dozy.mobile.feature.character.domain.usecase

import com.dozy.mobile.feature.character.domain.model.Character

fun interface CharacterIdGenerator {
    fun next(): Character.Id
}
