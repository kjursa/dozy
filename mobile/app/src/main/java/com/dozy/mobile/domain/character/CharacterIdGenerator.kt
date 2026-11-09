package com.dozy.mobile.domain.character

fun interface CharacterIdGenerator {
    fun next(): CharacterId
}
