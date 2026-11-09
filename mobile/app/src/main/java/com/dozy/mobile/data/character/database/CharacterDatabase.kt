package com.dozy.mobile.data.character.database

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [CharacterEntity::class], version = 1, exportSchema = true)
abstract class CharacterDatabase : RoomDatabase() {
    abstract fun characters(): CharacterDao
}
