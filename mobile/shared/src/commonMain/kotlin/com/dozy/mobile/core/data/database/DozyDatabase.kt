package com.dozy.mobile.core.data.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.place.data.database.PlaceDao
import com.dozy.mobile.feature.place.data.database.PlaceEntity
import com.dozy.mobile.feature.story.data.database.GenerationDao
import com.dozy.mobile.feature.story.data.database.GenerationEntity

@ConstructedBy(DozyDatabaseConstructor::class)
@Database(
    entities = [CharacterEntity::class, PlaceEntity::class, GenerationEntity::class],
    version = 8,
    exportSchema = true,
)
abstract class DozyDatabase : RoomDatabase() {
    abstract fun generations(): GenerationDao

    abstract fun places(): PlaceDao

    abstract fun characters(): CharacterDao
}

@Suppress("KotlinNoActualForExpect")
expect object DozyDatabaseConstructor : RoomDatabaseConstructor<DozyDatabase> {
    override fun initialize(): DozyDatabase
}
