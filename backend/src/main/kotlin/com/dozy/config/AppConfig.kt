package com.dozy.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(val database: DatabaseConfig, val avatars: AvatarConfig = AvatarConfig()) {
    companion object {
        fun from(config: ApplicationConfig) = AppConfig(
            DatabaseConfig(
                path = config.property("dozy.database.path").getString(),
                busyTimeoutMillis = config.property("dozy.database.busyTimeoutMillis").getString().toInt(),
            ),
            AvatarConfig(
                storagePath = config.propertyOrNull("dozy.avatars.storagePath")?.getString() ?: "data/assets",
                publicBaseUrl = config.propertyOrNull("dozy.avatars.publicBaseUrl")?.getString() ?: "https://localhost:8080",
                allowLocalHttp = config.propertyOrNull("dozy.avatars.allowLocalHttp")?.getString()?.toBooleanStrict() ?: false,
                mode = config.propertyOrNull("dozy.avatars.mode")?.getString()?.let(FakeAvatarMode::valueOf) ?: FakeAvatarMode.SUCCESS,
                delayMillis = config.propertyOrNull("dozy.avatars.delayMillis")?.getString()?.toLong() ?: 1000,
                concurrency = config.propertyOrNull("dozy.avatars.concurrency")?.getString()?.toInt() ?: 2,
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
