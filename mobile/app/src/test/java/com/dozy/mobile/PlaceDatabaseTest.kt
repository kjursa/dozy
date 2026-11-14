package com.dozy.mobile

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.dozy.api.client.place.PlacesClient
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.place.data.DefaultPlaceRepository
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

    @Test fun persistsPlaceOriginAvatarsAndPendingWritesAcrossReopen() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("places-test.db")
            val custom = place().toEntity()
            val remote = PlaceDto("remote", "Island", "Sandy cove", "SYSTEM", false, "Turtles").toEntity()
            val database = open(context)
            try {
                database.characters().upsert(character("hero").toEntity(true))
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
                        PlacesClient {
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
