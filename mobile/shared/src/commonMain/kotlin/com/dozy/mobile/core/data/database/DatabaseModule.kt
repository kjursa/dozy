package com.dozy.mobile.core.data.database

import androidx.room3.RoomDatabase
import org.koin.dsl.module
import org.koin.dsl.onClose

fun interface DatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<DozyDatabase>
}

val databaseModule =
    module {
        single {
            get<DatabaseBuilderFactory>()
                .create()
                .setDriver(get())
                .build()
        } onClose
            { it?.close() }
        single { get<DozyDatabase>().characters() }
        single { get<DozyDatabase>().places() }
        single { get<DozyDatabase>().generations() }
        single { get<DozyDatabase>().creationDrafts() }
    }
