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
    fun `records one validated migration across repeated startups`() {
        repeat(2) { SqliteDatabase(config).use { } }
        val flyway = Flyway.configure().dataSource(url, null, null).load()
        val applied = flyway.info().applied()
        assertEquals(1, applied.size)
        assertEquals("1", applied.single().version.version)
        assertEquals("V1__places.sql", applied.single().script)
        flyway.validate()
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
