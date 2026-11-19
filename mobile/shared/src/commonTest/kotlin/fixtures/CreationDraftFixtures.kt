package com.dozy.mobile

import com.dozy.mobile.core.data.database.CreationDraftDao
import com.dozy.mobile.core.data.database.CreationDraftEntity
import com.dozy.mobile.core.presentation.CreationDraft

internal class MemoryCreationDraft<State : Any> : CreationDraft<State> {
    private var state: State? = null

    override suspend fun load(): State? = state

    override suspend fun save(state: State) {
        this.state = state
    }

    override suspend fun clear() {
        state = null
    }
}

internal class FakeCreationDraftDao : CreationDraftDao {
    private val drafts = mutableMapOf<String, String>()

    override suspend fun load(form: String): String? = drafts[form]

    override suspend fun save(draft: CreationDraftEntity) {
        drafts[draft.form] = draft.payload
    }

    override suspend fun clear(form: String) {
        drafts.remove(form)
    }
}
