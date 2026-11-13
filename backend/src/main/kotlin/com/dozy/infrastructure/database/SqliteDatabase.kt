package com.dozy.infrastructure.database

import com.dozy.config.DatabaseConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteConfig

class SqliteDatabase(config: DatabaseConfig) : AutoCloseable {
    private val path = Path.of(config.path).toAbsolutePath()
    private val sqliteConfig = SQLiteConfig().apply {
        enforceForeignKeys(true)
        setBusyTimeout(config.busyTimeoutMillis)
        setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE)
    }
    private val database = Database.connect(
        getNewConnection = { sqliteConfig.createConnection("jdbc:sqlite:$path") },
    )

    init {
        Files.createDirectories(path.parent)
        try {
            transaction(database) { migrate() }
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    suspend fun <T> write(block: JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
        transaction(database) {
            maxAttempts = 1
            block()
        }
    }

    override fun close() {
        TransactionManager.closeAndUnregister(database)
    }

    private fun JdbcTransaction.migrate() {
        val version = checkNotNull(exec("PRAGMA user_version") { rows ->
            rows.next()
            rows.getInt(1)
        })
        check(version in 0..1) { "Unsupported database schema version" }
        if (version == 1) return
        val script = checkNotNull(this@SqliteDatabase.javaClass.getResource("/db/migration/V1__places.sql")).readText()
        script.split(';').map(String::trim).filter(String::isNotEmpty).forEach { exec(it) }
        exec("PRAGMA user_version = 1", explicitStatementType = StatementType.OTHER)
    }
}
