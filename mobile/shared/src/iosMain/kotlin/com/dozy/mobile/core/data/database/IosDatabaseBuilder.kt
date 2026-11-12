package com.dozy.mobile.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun iosDatabaseBuilder(): RoomDatabase.Builder<DozyDatabase> {
    val directory =
        requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask,
                null,
                true,
                null,
            ),
        )
    return Room.databaseBuilder<DozyDatabase>(name = "${directory.path}/dozy.db")
}
