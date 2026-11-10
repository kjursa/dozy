package com.dozy.mobile.feature.character.data.api

fun interface CharacterApi {
    /** True only when the server acknowledges persistence. False means local-only. */
    suspend fun save(character: CharacterDto): Boolean
}
