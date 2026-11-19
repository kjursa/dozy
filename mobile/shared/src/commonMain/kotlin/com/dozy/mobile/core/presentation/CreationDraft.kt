package com.dozy.mobile.core.presentation

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Stores a creation form independently of any server resource ID. */
interface CreationDraft<State : Any> {
    suspend fun load(): State?

    suspend fun save(state: State)

    suspend fun clear()
}

@OptIn(ExperimentalUuidApi::class)
internal fun newCreationRequestKey(): String = Uuid.generateV4().toString()
