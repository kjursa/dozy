package com.dozy.mobile.core.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity

@Database(
    entities = [CharacterEntity::class, PlaceEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class DozyDatabase : RoomDatabase() {
    abstract fun places(): PlaceDao

    abstract fun characters(): CharacterDao
}
