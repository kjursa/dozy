package com.dozy.mobile.core.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import com.dozy.mobile.feature.story.data.database.GenerationDao
import com.dozy.mobile.feature.story.data.database.GenerationEntity

@Database(
    entities = [CharacterEntity::class, PlaceEntity::class, GenerationEntity::class],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        ), AutoMigration(
            from = 2,
            to = 3,
        ), AutoMigration(
            from = 3,
            to = 4,
        ), AutoMigration(
            from = 4,
            to = 5,
        ), AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7), AutoMigration(from = 7, to = 8),
    ],
)
abstract class DozyDatabase : RoomDatabase() {
    abstract fun generations(): GenerationDao

    abstract fun places(): PlaceDao

    abstract fun characters(): CharacterDao
}
