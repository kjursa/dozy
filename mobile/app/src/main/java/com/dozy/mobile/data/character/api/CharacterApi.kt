package com.dozy.mobile.data.character.api

fun interface CharacterApi {
    /** True only when the server acknowledges persistence. False means local-only. */
    suspend fun save(character: CharacterDto): Boolean
}
