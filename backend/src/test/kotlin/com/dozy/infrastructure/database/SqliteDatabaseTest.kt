package com.dozy.infrastructure.database

import com.dozy.config.DatabaseConfig
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.junit.jupiter.api.io.TempDir

class SqliteDatabaseTest {
    @TempDir lateinit var directory: Path
    private val path get() = directory.resolve("dozy.db")
    private val url get() = "jdbc:sqlite:$path"
    private val config get() = DatabaseConfig(path.toString(), 5000)

    @Test
    fun `records validated migrations across repeated startups`() {
        repeat(2) { SqliteDatabase(config).use { } }
        val flyway = Flyway.configure().dataSource(url, null, null).load()
        val applied = flyway.info().applied()
        assertEquals(9, applied.size)
        assertEquals("1", applied.first().version.version)
        assertEquals("V1__places.sql", applied.first().script)
        assertEquals("9", applied.last().version.version)
        assertEquals("V9__story_publication.sql", applied.last().script)
        flyway.validate()
    }

    @Test
    fun `publication migration preserves existing stories and pending jobs`() {
        Flyway.configure().dataSource(url, null, null).target("8").load().migrate()
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("INSERT INTO installations (id) VALUES ('owner')")
                statement.executeUpdate(
                    """
                    INSERT INTO story_jobs (id, installation_id, operation, idempotency_key, canonical_request,
                                            configuration, stage, story_id, asset_id, created_at, title, text, execution_id)
                    VALUES ('done', 'owner', 'START_STORY_V1', 'done-key', '{}', '{}', 'READY', 'published', 'cover-1', 1, 'Title', 'Story', 'first'),
                           ('pending', 'owner', 'START_STORY_V1', 'pending-key', '{}', '{}', 'QUEUED', 'reserved', 'cover-2', 2, NULL, NULL, 'second')
                    """.trimIndent(),
                )
                statement.executeUpdate("INSERT INTO stories VALUES ('published', 'done', 'owner', 'Title', 'Story', 'cover', '{}', 3)")
                statement.executeUpdate("INSERT INTO story_executions VALUES ('second', 'owner', 'pending', 'pending-key', 'START_STORY_V1', 1, 'QUEUED')")
                statement.executeUpdate("INSERT INTO library_snapshots VALUES ('snapshot', 'owner', 1, '[]')")
                statement.executeUpdate("INSERT INTO library_cursors VALUES ('cursor', 'snapshot', 1)")
            }
        }
        SqliteDatabase(config).use { }
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id, text FROM stories WHERE job_id = 'done'").use {
                    assertEquals(true, it.next())
                    assertEquals("published", it.getString("id"))
                    assertEquals("Story", it.getString("text"))
                }
                statement.executeQuery(
                    "SELECT j.stage FROM story_jobs j JOIN story_executions e ON e.job_id = j.id AND e.id = j.execution_id WHERE j.id = 'pending'",
                ).use {
                    assertEquals(true, it.next())
                    assertEquals("QUEUED", it.getString("stage"))
                }
                statement.executeQuery("PRAGMA foreign_key_check").use { assertEquals(false, it.next()) }
                statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE name IN ('library_snapshots', 'library_cursors')").use {
                    assertEquals(true, it.next())
                    assertEquals(0, it.getInt(1))
                }
            }
        }
    }

    @Test
    fun `fails startup on modified migration checksum`() {
        SqliteDatabase(config).use { }
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use {
                it.executeUpdate("UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version = '1'")
            }
        }
        assertFailsWith<FlywayException> { SqliteDatabase(config) }
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM places").use {
                    it.next()
                    assertEquals(3, it.getInt(1))
                }
            }
        }
    }

    @Test
    fun `refuses to silently baseline an unmanaged database`() {
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { it.execute("CREATE TABLE existing_data(value TEXT)") }
        }
        assertFailsWith<FlywayException> { SqliteDatabase(config) }
    }
}
