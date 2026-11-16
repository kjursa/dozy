package com.dozy.mobile

import com.dozy.api.client.ApiDisabledException
import com.dozy.api.client.TechnicalError
import com.dozy.api.client.character.v1.CharactersDeleteResult
import com.dozy.api.client.character.v1.CharactersListResult
import com.dozy.api.client.character.v1.CharactersSaveResult
import com.dozy.api.client.characteravatar.v1.AvatarStartResult
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.UuidCharacterIdGenerator
import com.dozy.mobile.feature.character.data.database.CharacterDao
import com.dozy.mobile.feature.character.data.database.CharacterEntity
import com.dozy.mobile.feature.character.data.toDomain
import com.dozy.mobile.feature.character.data.toDto
import com.dozy.mobile.feature.character.data.toEntity
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.model.CharacterSync
import fixtures.CharactersClientFixture
import fixtures.avatarClients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class CharacterDataTest {
    private fun repository(
        dao: FakeDao,
        client: CharactersClientFixture = CharactersClientFixture(),
    ) = DefaultCharacterRepository(
        dao,
        client,
        avatarClients {
            AvatarStartResult.TechnicalError(TechnicalError.Communication(ApiDisabledException()))
        },
    )

    @Test fun newIdsUseSharedUuidV7Facility() {
        val ids = List(100) { UuidCharacterIdGenerator().next().value }
        assertEquals(100, ids.distinct().size)
        assertTrue(ids.all { it.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) })
    }

    @Test fun mapsAllFieldsAndPreservesHistoricalValues() {
        val original = character()
        assertEquals(original, original.toEntity(true).toDomain())
        assertEquals(CharacterDto("1", "Oliver", "Curious and brave", "Curly brown hair", "CHILD"), original.toDto())
        val legacy = CharacterEntity("legacy", "x".repeat(55), "", "legacy", "CHILD", false)
        assertEquals(legacy, legacy.toDomain().toEntity(false))
        assertEquals(legacy.name, legacy.toDomain().toDto().name)
    }

    @Test fun persistsBeforeSendingAndOnlyAcknowledgedRowsBecomeSynced() =
        runTest {
            val dao = FakeDao()
            val client =
                CharactersClientFixture().apply {
                    onSave = { id, request ->
                        assertEquals("1", id)
                        assertEquals("Oliver", request.name)
                        assertTrue(
                            dao.rows.value
                                .single()
                                .pendingSync,
                        )
                        CharactersSaveResult.Success
                    }
                }
            val repository = repository(dao, client)
            assertEquals(CharacterSync.SYNCED, repository.save(character()).sync)
            assertFalse(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
            assertEquals(listOf(character()), repository.observe().first())
        }

    @Test fun rejectionsAndExceptionsKeepDurablePendingRows() =
        runTest {
            for (failure in listOf(false, true)) {
                val dao = FakeDao()
                val client =
                    CharactersClientFixture().apply {
                        onSave = { _, _ -> if (failure) CharactersSaveResult.TechnicalError(TechnicalError.Communication(IllegalStateException("offline"))) else CharactersSaveResult.ApiError(CharactersSaveError.Conflict) }
                    }
                assertEquals(CharacterSync.PENDING, repository(dao, client).save(character()).sync)
                assertTrue(
                    dao.rows.value
                        .single()
                        .pendingSync,
                )
            }
        }

    @Test fun diskFailurePreventsHttpAndMetadataFailureKeepsPending() =
        runTest {
            val dao = FakeDao().apply { failAt = 1 }
            var calls = 0
            val client =
                CharactersClientFixture().apply {
                    onSave = { _, _ ->
                        calls++
                        CharactersSaveResult.Success
                    }
                }
            assertFailsWith<IllegalStateException> { repository(dao, client).save(character()) }
            assertEquals(0, calls)
            dao.failAt = 3
            assertEquals(CharacterSync.PENDING, repository(dao, client).save(character()).sync)
            assertTrue(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
        }

    @Test fun cancellationPropagatesAfterDurableSave() =
        runTest {
            val dao = FakeDao()
            val client = CharactersClientFixture().apply { onSave = { _, _ -> throw CancellationException() } }
            assertFailsWith<CancellationException> { repository(dao, client).save(character()) }
            assertTrue(
                dao.rows.value
                    .single()
                    .pendingSync,
            )
        }

    @Test fun refreshRetriesPendingWritesAndReplacesAuthoritativeCollection() =
        runTest {
            val dao = FakeDao()
            dao.upsert(character().toEntity(true))
            dao.upsert(character("removed").toEntity(false))
            var saved = 0
            val client =
                CharactersClientFixture().apply {
                    onSave = { _, _ ->
                        saved++
                        CharactersSaveResult.Success
                    }
                    onList = { CharactersListResult.Success(listOf(character().toDto(), character("2", "Luna").toDto())) }
                }
            assertTrue(repository(dao, client).refresh())
            assertEquals(1, saved)
            assertEquals(
                setOf("1", "2"),
                dao.rows.value
                    .map { it.id }
                    .toSet(),
            )
            assertTrue(dao.rows.value.none { it.pendingSync })
            client.onList = { CharactersListResult.Success(emptyList()) }
            assertTrue(repository(dao, client).refresh())
            assertTrue(dao.rows.value.isEmpty())
        }

    @Test fun failedWritesProtectPendingEditsFromStaleSnapshots() =
        runTest {
            val dao = FakeDao()
            dao.upsert(character().toEntity(true))
            dao.upsert(character("removed").toEntity(false))
            val client =
                CharactersClientFixture().apply {
                    onList = { CharactersListResult.Success(listOf(character(name = "Stale").toDto(), character("2", "Luna").toDto())) }
                }
            assertTrue(repository(dao, client).refresh())
            assertEquals(
                setOf("Oliver", "Luna"),
                repository(dao)
                    .observe()
                    .first()
                    .map { it.details.name.value }
                    .toSet(),
            )
            assertTrue(
                dao.rows.value
                    .single { it.id == "1" }
                    .pendingSync,
            )
        }

    @Test fun failedDisabledMalformedAndCancelledCollectionsPreserveRows() =
        runTest {
            val dao = FakeDao()
            dao.upsert(character().toEntity(false))
            val client = CharactersClientFixture()
            assertFalse(repository(dao, client).refresh())
            client.onList = { CharactersListResult.TechnicalError(TechnicalError.Communication(ApiDisabledException())) }
            assertFalse(repository(dao, client).refresh())
            client.onList = { throw CancellationException() }
            assertFailsWith<CancellationException> { repository(dao, client).refresh() }
            client.onList = { CharactersListResult.Success(listOf(character().toDto().copy(type = "UNKNOWN"))) }
            assertFailsWith<IllegalArgumentException> { repository(dao, client).refresh() }
            client.onList = { CharactersListResult.Success(listOf(character().toDto(), character().toDto())) }
            assertFailsWith<IllegalArgumentException> { repository(dao, client).refresh() }
            assertEquals(listOf(character()), repository(dao).observe().first())
        }

    @Test fun deletesLocallyBeforeHttpAndRetainsTombstonesAcrossAcknowledgements() =
        runTest {
            val dao = FakeDao()
            dao.upsert(character().toEntity(false))
            val client =
                CharactersClientFixture().apply {
                    onDelete = {
                        assertTrue(
                            dao.rows.value
                                .single()
                                .deleted,
                        )
                        CharactersDeleteResult.Success
                    }
                    onList = { CharactersListResult.Success(listOf(character().toDto())) }
                }
            val repository = repository(dao, client)
            assertEquals(CharacterSync.SYNCED, repository.delete(character().id))
            assertTrue(repository.refresh())
            assertNull(repository.observe(character().id).first())
            assertEquals(listOf("1"), dao.deletedIds())
            assertNull(repository.update(character()))
            assertFailsWith<IllegalArgumentException> { repository.save(character()) }
        }

    @Test fun failedOrCancelledDeletionCannotResurrectRows() =
        runTest {
            for (cancel in listOf(false, true)) {
                val dao = FakeDao()
                dao.upsert(character().toEntity(false))
                val client =
                    CharactersClientFixture().apply {
                        onDelete = { if (cancel) throw CancellationException() else CharactersDeleteResult.TechnicalError(TechnicalError.Communication(IllegalStateException("offline"))) }
                    }
                val repository = repository(dao, client)
                if (cancel) {
                    assertFailsWith<CancellationException> { repository.delete(character().id) }
                } else {
                    assertEquals(CharacterSync.PENDING, repository.delete(character().id))
                }
                assertNull(repository.observe(character().id).first())
                assertEquals(listOf("1"), dao.deletedIds())
            }
        }

    @Test fun editsAcknowledgeSafelyAndCannotUpdateMissingOrDeletedRows() =
        runTest {
            val dao = FakeDao()
            dao.upsert(character().toEntity(false))
            val client = CharactersClientFixture().apply { onSave = { _, _ -> CharactersSaveResult.Success } }
            val repository = repository(dao, client)
            val edited = character(name = "Edited").copy(avatar = avatar())
            assertEquals(CharacterSync.SYNCED, repository.update(edited)?.sync)
            assertEquals(edited, repository.observe(edited.id).first())
            assertNull(repository.update(character("missing")))
            dao.markDeleted("1")
            assertNull(repository.update(edited))
            assertEquals(1, dao.rows.value.size)
        }

    @Test fun lateSaveAcknowledgementCannotResurrectConcurrentDeletion() =
        runTest {
            val dao = FakeDao()
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val client =
                CharactersClientFixture().apply {
                    onSave = { _, _ ->
                        started.complete(Unit)
                        release.await()
                        CharactersSaveResult.Success
                    }
                }
            val repository = repository(dao, client)
            val save = async { repository.save(character()) }
            started.await()
            val delete = async { repository.delete(character().id) }
            release.complete(Unit)
            save.await()
            delete.await()
            assertNull(repository.observe(character().id).first())
            assertTrue(
                dao.rows.value
                    .single()
                    .deleted,
            )
        }

    @Test fun bundledAvatarsRemainLocalAndAreNeverUploadedEvenDuringRefresh() =
        runTest {
            val dao = FakeDao()
            val client =
                CharactersClientFixture().apply {
                    onSave = { _, _ -> error("Bundled avatar must not be sent") }
                    onList = { CharactersListResult.Success(emptyList()) }
                }
            val preview =
                character().copy(
                    avatar = Character.Avatar(Character.Avatar.Id("fake:oliver"), Character.Avatar.Url("bundled-avatar://oliver")),
                )
            val repository = repository(dao, client)
            assertEquals(CharacterSync.PENDING, repository.save(preview).sync)
            assertTrue(repository.refresh())
            assertEquals(preview, repository.observe(preview.id).first())
        }

    private class FakeDao : CharacterDao {
        val rows = MutableStateFlow(emptyList<CharacterEntity>())
        var failAt = 0
        var calls = 0

        override suspend fun pendingIds() = rows.value.filter { it.pendingSync }.map { it.id }

        override suspend fun deleteSynced() {
            rows.value = rows.value.filter { it.pendingSync }
        }

        override fun observe() = rows.map { it.filterNot { row -> row.deleted } }

        override fun observe(id: String) = rows.map { it.firstOrNull { row -> row.id == id && !row.deleted } }

        override suspend fun deletedIds() = rows.value.filter { it.deleted }.map { it.id }

        override suspend fun markDeleted(id: String) {
            rows.value = rows.value.map { if (it.id == id) it.copy(deleted = true, pendingSync = true) else it }
        }

        override suspend fun updateExisting(
            id: String,
            name: String,
            personality: String,
            appearance: String,
            type: String,
            avatarId: String?,
            avatarUrl: String?,
        ): Int {
            if (rows.value.none { it.id == id && !it.deleted }) return 0
            upsert(CharacterEntity(id, name, personality, appearance, type, true, avatarId, avatarUrl))
            return 1
        }

        override suspend fun upsert(character: CharacterEntity) {
            calls++
            if (calls == failAt) error("disk full")
            rows.value = rows.value.filterNot { it.id == character.id } + character
        }
    }
}
