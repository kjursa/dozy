package com.dozy.mobile.feature.character.domain

fun interface CharacterIdGenerator {
    fun next(): CharacterId
}
