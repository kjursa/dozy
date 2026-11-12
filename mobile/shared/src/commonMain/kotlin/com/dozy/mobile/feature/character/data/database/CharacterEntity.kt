package com.dozy.mobile.feature.character.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: String,
    val pendingSync: Boolean,
    val avatarId: String? = null,
    val avatarUrl: String? = null,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)
