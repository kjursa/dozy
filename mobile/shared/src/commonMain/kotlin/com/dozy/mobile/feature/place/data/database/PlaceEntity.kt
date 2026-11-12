package com.dozy.mobile.feature.place.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val inhabitants: String?,
    val ownership: String,
    val editable: Boolean,
    val local: Boolean,
    @ColumnInfo(defaultValue = "1") val pendingSync: Boolean = local,
)
