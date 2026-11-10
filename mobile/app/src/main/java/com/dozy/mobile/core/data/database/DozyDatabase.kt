package com.dozy.mobile.core.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity

@Database(entities = [CharacterEntity::class], version = 1, exportSchema = true)
abstract class DozyDatabase : RoomDatabase() {
    abstract fun characters(): CharacterDao
}
