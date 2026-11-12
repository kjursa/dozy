package com.kjursa.foundation.compose.mvi

import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Stores a serialized state under one key. Serialization rules belong to the supplied serializer. */
class SavedStatePersistence<State : Any>(
    private val handle: SavedStateHandle,
    private val serializer: KSerializer<State>,
    private val key: String = DEFAULT_KEY,
    private val json: Json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        },
) : StatePersistence<State> {
    @MainThread
    override fun restore(defaultState: State): State {
        val encoded = handle.get<String>(key) ?: return defaultState
        return try {
            json.decodeFromString(serializer, encoded)
        } catch (_: SerializationException) {
            // Stale or incompatible snapshots should not prevent opening a screen.
            handle.remove<String>(key)
            defaultState
        }
    }

    @MainThread
    override fun save(state: State) {
        val encoded = json.encodeToString(serializer, state)
        if (handle.get<String>(key) != encoded) handle[key] = encoded
    }

    companion object {
        const val DEFAULT_KEY = "STATE"
    }
}
