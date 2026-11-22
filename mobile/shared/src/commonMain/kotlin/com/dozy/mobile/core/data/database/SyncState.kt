package com.dozy.mobile.core.data.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val collection: String,
    val value: String?,
)

interface SyncStateQueries {
    @Query("SELECT value FROM sync_state WHERE collection = :collection")
    suspend fun syncValue(collection: String): String?

    @Upsert
    suspend fun saveSyncValue(state: SyncState)
}

internal val syncStateMigration =
    object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE sync_state (collection TEXT NOT NULL PRIMARY KEY, value TEXT)")
        }
    }
