package com.dozy.infrastructure.database

import com.dozy.config.DatabaseConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource

class SqliteDatabase(config: DatabaseConfig) : AutoCloseable {
    private val path = Path.of(config.path).toAbsolutePath()
    private val sqliteConfig = SQLiteConfig().apply {
        enforceForeignKeys(true)
        setBusyTimeout(config.busyTimeoutMillis)
        setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE)
    }
    private val dataSource = SQLiteDataSource(sqliteConfig).apply {
        url = "jdbc:sqlite:$path"
    }
    private val database = Database.connect(dataSource)

    init {
        Files.createDirectories(path.parent)
        try {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load()
                .migrate()
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
}
