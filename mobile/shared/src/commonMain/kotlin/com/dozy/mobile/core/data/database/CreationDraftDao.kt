package com.dozy.mobile.core.data.database

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

@Entity(tableName = "creation_drafts")
data class CreationDraftEntity(
    @PrimaryKey val form: String,
    val payload: String,
)

@Dao
interface CreationDraftDao {
    @Query("SELECT payload FROM creation_drafts WHERE form = :form")
    suspend fun load(form: String): String?

    @Upsert
    suspend fun save(draft: CreationDraftEntity)

    @Query("DELETE FROM creation_drafts WHERE form = :form")
    suspend fun clear(form: String)
}
