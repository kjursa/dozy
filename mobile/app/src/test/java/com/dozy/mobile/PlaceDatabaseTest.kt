package com.dozy.mobile

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.character.data.toEntity
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

    @Test fun migratesVersionFivePreservingDeletionMarkersAndPendingPlaces() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("places-test.db")
            val path = context.getDatabasePath("places-test.db")
            path.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(
                    "CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, personality TEXT NOT NULL, appearance TEXT NOT NULL, type TEXT NOT NULL, pendingSync INTEGER NOT NULL, avatarId TEXT, avatarUrl TEXT, deleted INTEGER NOT NULL DEFAULT 0)",
                )
                old.execSQL(
                    "CREATE TABLE places (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, inhabitants TEXT, ownership TEXT NOT NULL, editable INTEGER NOT NULL, local INTEGER NOT NULL, pendingSync INTEGER NOT NULL DEFAULT 1)",
                )
                old.execSQL(
                    "INSERT INTO characters VALUES ('deleted', 'Oliver', 'Curious', 'Green hat', 'CHILD', 1, 'asset', 'https://example.test/avatar.png', 1)",
                )
                old.execSQL("INSERT INTO places VALUES ('custom', 'Garden', 'Wooden gate', NULL, 'CUSTOM', 1, 1, 1)")
                old.version = 5
            }
            try {
                val database = open(context)
                try {
                    assertTrue(
                        database
                            .characters()
                            .observe()
                            .first()
                            .isEmpty(),
                    )
                    assertEquals(listOf("deleted"), database.characters().deletedIds())
                    assertEquals(listOf("deleted"), database.characters().pendingIds())
                    assertEquals(listOf("custom"), database.places().pending().map { it.id })
                    assertTrue(
                        database
                            .generations()
                            .observe()
                            .first()
                            .isEmpty(),
                    )
                    database.generations().upsert(
                        com.dozy.mobile.feature.story.data.database
                            .GenerationEntity("request", "snapshot", "job", "QUEUED"),
                    )
                } finally {
                    database.close()
                }
                val reopened = open(context)
                try {
                    assertEquals("snapshot", reopened.generations().find("request")?.configuration)
                    assertEquals("job", reopened.generations().find("request")?.jobId)
                    assertEquals(listOf("deleted"), reopened.characters().deletedIds())
                    assertEquals(listOf("custom"), reopened.places().pending().map { it.id })
                } finally {
                    reopened.close()
                }
            } finally {
                context.deleteDatabase("places-test.db")
            }
        }

    @Test fun migratesVersionThreePreservingAvatarsAndQueuesOnlyLocalPlaces() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("places-test.db")
            val path = context.getDatabasePath("places-test.db")
            path.parentFile?.mkdirs()
            // Version three on main already contains avatars, but not place sync metadata.
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(
                    "CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, personality TEXT NOT NULL, appearance TEXT NOT NULL, type TEXT NOT NULL, pendingSync INTEGER NOT NULL, avatarId TEXT, avatarUrl TEXT)",
                )
                old.execSQL(
                    "CREATE TABLE places (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, inhabitants TEXT, ownership TEXT NOT NULL, editable INTEGER NOT NULL, local INTEGER NOT NULL)",
                )
                old.execSQL(
                    "INSERT INTO characters VALUES ('hero', 'Oliver', 'Curious', 'Green hat', 'CHILD', 1, 'asset', 'https://example.test/avatar.png')",
                )
                old.execSQL("INSERT INTO places VALUES ('custom', 'Garden', 'Wooden gate', 'Robin', 'CUSTOM', 1, 1)")
                old.execSQL("INSERT INTO places VALUES ('remote', 'Island', 'Sandy cove', NULL, 'PREDEFINED', 0, 0)")
                old.version = 3
            }
            try {
                val database = open(context)
                try {
                    val character =
                        database
                            .characters()
                            .observe()
                            .first()
                            .single()
                    assertEquals("asset", character.avatarId)
                    assertEquals("https://example.test/avatar.png", character.avatarUrl)
                    assertEquals(
                        setOf("custom", "remote"),
                        database
                            .places()
                            .observe()
                            .first()
                            .map { it.id }
                            .toSet(),
                    )
                    assertEquals("Robin", database.places().find("custom")?.inhabitants)
                    assertEquals(listOf("custom"), database.places().pending().map { it.id })
                    database.generations().upsert(
                        com.dozy.mobile.feature.story.data.database
                            .GenerationEntity("request", "snapshot", "job", "QUEUED"),
                    )
                } finally {
                    database.close()
                }
                val reopened = open(context)
                try {
                    assertEquals(listOf("custom"), reopened.places().pending().map { it.id })
                    assertEquals("snapshot", reopened.generations().find("request")?.configuration)
                    assertEquals("job", reopened.generations().find("request")?.jobId)
                    assertEquals(
                        "asset",
                        reopened
                            .characters()
                            .observe()
                            .first()
                            .single()
                            .avatarId,
                    )
                } finally {
                    reopened.close()
                }
            } finally {
                context.deleteDatabase("places-test.db")
            }
        }

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
                        .first { it.id == "hero" }
                        .name,
                )
                assertTrue(
                    database
                        .places()
                        .observe()
                        .first()
                        .isEmpty(),
                )
                database.characters().upsert(character("avatar-hero").copy(avatar = avatar()).toEntity(true))
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
                assertEquals(
                    "asset",
                    reopened
                        .characters()
                        .observe()
                        .first()
                        .first { it.id == "avatar-hero" }
                        .avatarId,
                )
                assertEquals(
                    "https://example.test/avatar.png",
                    reopened
                        .characters()
                        .observe()
                        .first()
                        .first { it.id == "avatar-hero" }
                        .avatarUrl,
                )
                assertEquals("Turtles", reopened.places().find("remote")?.inhabitants)
                assertEquals(listOf(custom), reopened.places().pending())
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
                        .first { it.id == "hero" }
                        .name,
                )
            } finally {
                reopened.close()
                context.deleteDatabase("places-test.db")
            }
        }
}
