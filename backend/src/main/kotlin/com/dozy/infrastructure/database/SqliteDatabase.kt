package com.dozy.infrastructure.database

import com.dozy.config.DatabaseConfig
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteDatabase(private val config: DatabaseConfig) {
    private val path = Path.of(config.path).toAbsolutePath()

    init {
        Files.createDirectories(path.parent)
        transaction { connection -> migrate(connection) }
    }

    suspend fun <T> write(block: (Connection) -> T): T = withContext(Dispatchers.IO) { transaction(block) }

    private fun connection(): Connection {
        val connection = DriverManager.getConnection("jdbc:sqlite:$path")
        try {
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA busy_timeout = ${config.busyTimeoutMillis}")
            }
            return connection
        } catch (failure: Throwable) {
            connection.close()
            throw failure
        }
    }

    private fun <T> transaction(block: (Connection) -> T): T = connection().use { connection ->
        connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
        try {
            val result = block(connection)
            connection.createStatement().use { it.execute("COMMIT") }
            result
        } catch (failure: Throwable) {
            runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
            throw failure
        }
    }

    private fun migrate(connection: Connection) {
        val version = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA user_version").use {
                it.next()
                it.getInt(1)
            }
        }
        check(version in 0..1) { "Unsupported database schema version" }
        if (version == 1) return
        val script = checkNotNull(javaClass.getResource("/db/migration/V1__places.sql")).readText()
        connection.createStatement().use { statement ->
            script.split(';').map(String::trim).filter(String::isNotEmpty).forEach(statement::execute)
            statement.execute("PRAGMA user_version = 1")
        }
    }
}
