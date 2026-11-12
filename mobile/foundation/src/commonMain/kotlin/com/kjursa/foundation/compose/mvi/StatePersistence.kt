package com.kjursa.foundation.compose.mvi

/** Optional persistence for the small, restorable part of a screen state. */
interface StatePersistence<State : Any> {
    fun restore(defaultState: State): State

    fun save(state: State)
}
