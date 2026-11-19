package com.dozy.mobile.core.data.database

import com.dozy.mobile.core.presentation.CreationDraft
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal class RoomCreationDraft<State : Any>(
    private val dao: CreationDraftDao,
    private val form: String,
    private val serializer: KSerializer<State>,
) : CreationDraft<State> {
    override suspend fun load(): State? = dao.load(form)?.let { json.decodeFromString(serializer, it) }

    override suspend fun save(state: State) {
        dao.save(CreationDraftEntity(form, json.encodeToString(serializer, state)))
    }

    override suspend fun clear() {
        dao.clear(form)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
