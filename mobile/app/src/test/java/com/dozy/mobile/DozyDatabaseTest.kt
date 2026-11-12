package com.dozy.mobile

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.core.data.database.androidDatabaseBuilder
import com.dozy.mobile.di.androidModule
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.di.characterModule
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.place.data.toEntity
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class DozyDatabaseTest {
    @Test fun extractionReopensTheInstalledVersionEightDatabaseAtTheSamePath() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("dozy.db")
            val character = character().toEntity(true)
            val place = place().toEntity()
            val generation = GenerationEntity("existing-key", "immutable-snapshot", "existing-job", "READY", result = "saved-story")
            // The builder used before extraction writes the installed database.
            val original = Room.databaseBuilder<DozyDatabase>(context, "dozy.db").setDriver(AndroidSQLiteDriver()).build()
            try {
                original.characters().upsert(character)
                original.places().upsert(place)
                original.generations().upsert(generation)
            } finally {
                original.close()
            }
            val path = context.getDatabasePath("dozy.db")
            val identity =
                android.database.sqlite.SQLiteDatabase
                    .openDatabase(
                        path.path,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                    ).use {
                        assertEquals(8, it.version)
                        it.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null).use { cursor ->
                            assertTrue(cursor.moveToFirst())
                            cursor.getString(0)
                        }
                    }
            // The production shared Android boundary must find that same file and data.
            val extracted = androidDatabaseBuilder(context).setDriver(AndroidSQLiteDriver()).build()
            try {
                assertEquals(
                    character,
                    extracted
                        .characters()
                        .observe()
                        .first()
                        .single(),
                )
                assertEquals(place, extracted.places().find(place.id))
                assertEquals(generation, extracted.generations().find("existing-key"))
                assertEquals("2978184b9f87fa22118d5550770355e6", identity)
            } finally {
                extracted.close()
                context.deleteDatabase("dozy.db")
            }
        }

    @Test fun deletionTombstoneSurvivesReopenAndProtectsOnlyItsOwnId() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("dozy.db")

            fun graph() =
                koinApplication {
                    androidContext(context)
                    modules(androidModule, characterModule, module { single<SQLiteDriver> { AndroidSQLiteDriver() } })
                }
            val first = graph()
            try {
                val dao = first.koin.get<CharacterDao>()
                dao.upsert(character().toEntity(false))
                dao.upsert(character("2", "Luna").toEntity(false))
                val repository = first.koin.get<CharacterRepository>()
                val edited = character(name = "Edited").copy(avatar = avatar())
                assertEquals(CharacterSync.PENDING, repository.update(edited)?.sync)
                assertEquals(edited.toEntity(true), dao.observe("1").first())
                assertEquals(null, repository.update(character("missing")))
                repository.delete(Character.Id("1"))
                assertEquals(null, repository.update(edited))
                assertEquals(listOf("2"), dao.observe().first().map { it.id })
                assertEquals(null, dao.observe("1").first())
            } finally {
                first.close()
            }
            val reopened = graph()
            try {
                val dao = reopened.koin.get<CharacterDao>()
                assertEquals(listOf("1"), dao.deletedIds())
                dao.replaceSnapshot(listOf(character().toEntity(false), character("2", "Luna").toEntity(false)))
                assertEquals(listOf("2"), dao.observe().first().map { it.id })
            } finally {
                reopened.close()
                context.deleteDatabase("dozy.db")
            }
        }

    @Test fun sharedDatabasePersistsAllCharacterFieldsAcrossKoinLifetimes() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("dozy.db")
            val character =
                Character(
                    Character.Id("persistent-id"),
                    Character.Details(
                        Character.Name("Żółwik"),
                        Character.Personality("Curious\nand kind"),
                        Character.Appearance("A green shell and a blue hat"),
                        Character.Type.ANIMAL,
                    ),
                    avatar(),
                )

            fun graph() =
                koinApplication {
                    androidContext(context)
                    // Robolectric supplies real native SQLite through the Android framework driver.
                    modules(androidModule, characterModule, module { single<SQLiteDriver> { AndroidSQLiteDriver() } })
                }
            val app = graph()
            val database = app.koin.get<DozyDatabase>()
            try {
                assertSame(database, app.koin.get<DozyDatabase>())
                assertSame(database.characters(), app.koin.get<CharacterDao>())
                assertTrue(
                    database
                        .characters()
                        .observe()
                        .first()
                        .isEmpty(),
                )
                val saved = app.koin.get<CharacterRepository>().save(character)
                assertEquals(CharacterSync.PENDING, saved.sync)
                assertTrue(context.getDatabasePath("dozy.db").isFile)
                val dao = database.characters()
                dao.upsert(character("synced").toEntity(false))
                val emission =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        dao.observe().first { rows -> rows.size == 2 && rows.none { it.id == "synced" } }
                    }
                dao.replaceSnapshot(listOf(character.toEntity(false), character("other", "Luna").toEntity(false)))
                val snapshot = emission.await()
                assertEquals(setOf("persistent-id", "other"), snapshot.map { it.id }.toSet())
                assertTrue(snapshot.single { it.id == "persistent-id" }.pendingSync)
                dao.replaceSnapshot(emptyList())
                assertEquals(listOf(character.toEntity(true)), dao.observe().first { it.size == 1 })
            } finally {
                app.close()
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking { database.characters().upsert(character.toEntity(pendingSync = true)) }
            }

            val reopened = graph()
            val reopenedDatabase = reopened.koin.get<DozyDatabase>()
            try {
                val dao = reopened.koin.get<CharacterDao>()
                assertSame(reopenedDatabase.characters(), dao)
                assertEquals(
                    listOf(
                        CharacterEntity(
                            "persistent-id",
                            "Żółwik",
                            "Curious\nand kind",
                            "A green shell and a blue hat",
                            "ANIMAL",
                            true,
                            "asset",
                            "https://example.test/avatar.png",
                        ),
                    ),
                    dao.observe().first(),
                )
                assertEquals(
                    listOf(character),
                    reopened.koin
                        .get<CharacterRepository>()
                        .observe()
                        .first(),
                )
                dao.upsert(
                    dao
                        .observe()
                        .first()
                        .single()
                        .copy(pendingSync = false),
                )
            } finally {
                reopened.close()
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking { reopenedDatabase.characters().upsert(character.toEntity(pendingSync = true)) }
            }

            val finalApp = graph()
            try {
                assertFalse(
                    finalApp.koin
                        .get<CharacterDao>()
                        .observe()
                        .first()
                        .single()
                        .pendingSync,
                )
            } finally {
                finalApp.close()
                context.deleteDatabase("dozy.db")
            }
        }
}
