package com.dozy.mobile

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
import com.dozy.mobile.feature.place.data.api.PlaceApi
import com.dozy.mobile.feature.place.data.api.PlaceDto
import com.dozy.mobile.feature.place.data.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PlaceDatabaseTest {
    private fun open(context: Context) =
        Room
            .databaseBuilder<DozyDatabase>(context, "places-test.db")
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Test fun migratesVersionOneWithoutLosingCharactersAndPersistsPlacesAcrossReopen() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("places-test.db")
            val path = context.getDatabasePath("places-test.db")
            path.parentFile?.mkdirs()
            // The exact version-one schema, before places existed.
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(
                    "CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, personality TEXT NOT NULL, appearance TEXT NOT NULL, type TEXT NOT NULL, pendingSync INTEGER NOT NULL)",
                )
                old.execSQL("INSERT INTO characters VALUES ('hero', 'Oliver', 'Curious', 'Green hat', 'CHILD', 1)")
                old.version = 1
            }
            val custom = place().toEntity()
            val remote = PlaceDto("remote", "Island", "Sandy cove", "PREDEFINED", false, "Turtles").toEntity()
            val database = open(context)
            try {
                assertEquals(
                    "Oliver",
                    database
                        .characters()
                        .observe()
                        .first()
                        .single()
                        .name,
                )
                assertTrue(
                    database
                        .places()
                        .observe()
                        .first()
                        .isEmpty(),
                )
                database.places().upsert(custom)
                database.places().replaceRemote(listOf(remote, remote.copy(id = custom.id, name = "Server overwrite")))
                assertEquals(custom, database.places().find(custom.id))
            } finally {
                database.close()
            }
            val reopened = open(context)
            try {
                assertEquals(
                    setOf(custom, remote),
                    reopened
                        .places()
                        .observe()
                        .first()
                        .toSet(),
                )
                assertEquals("Turtles", reopened.places().find("remote")?.inhabitants)
                val repository =
                    DefaultPlaceRepository(
                        reopened.places(),
                        PlaceApi {
                            error("Local reads must not request HTTP")
                        },
                        listOf(place("builtin:woods", custom = false)),
                    )
                assertEquals(3, repository.observe().first().size)
                reopened.places().replaceRemote(emptyList())
                assertEquals(listOf(custom), reopened.places().observe().first())
                assertEquals(
                    "Oliver",
                    reopened
                        .characters()
                        .observe()
                        .first()
                        .single()
                        .name,
                )
            } finally {
                reopened.close()
                context.deleteDatabase("places-test.db")
            }
        }
}
