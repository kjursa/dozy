package com.dozy.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(val database: DatabaseConfig) {
    companion object {
        fun from(config: ApplicationConfig) = AppConfig(
            DatabaseConfig(
                path = config.property("dozy.database.path").getString(),
                busyTimeoutMillis = config.property("dozy.database.busyTimeoutMillis").getString().toInt(),
            ),
        )
    }
}

data class DatabaseConfig(val path: String, val busyTimeoutMillis: Int) {
    init {
        require(path.isNotBlank() && path != ":memory:") { "Use a persistent SQLite file path" }
        require(busyTimeoutMillis in 1..60000) { "Database busy timeout must be between 1 and 60000 ms" }
    }
}
