package com.dozy.mobile.core.data.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/** Keep the installed application's database path across module extraction. */
fun androidDatabaseBuilder(context: Context): RoomDatabase.Builder<DozyDatabase> =
    Room.databaseBuilder<DozyDatabase>(context.applicationContext, "dozy.db")
