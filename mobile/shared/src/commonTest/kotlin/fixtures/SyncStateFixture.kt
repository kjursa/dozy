package fixtures

import com.dozy.mobile.core.data.database.SyncState
import com.dozy.mobile.core.data.database.SyncStateQueries

internal class FakeSyncStateQueries : SyncStateQueries {
    private val values = mutableMapOf<String, String?>()

    override suspend fun syncValue(collection: String) = values[collection]

    override suspend fun saveSyncValue(state: SyncState) {
        values[state.collection] = state.value
    }
}
