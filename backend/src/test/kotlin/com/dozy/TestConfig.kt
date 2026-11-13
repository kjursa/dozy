package com.dozy

import io.ktor.server.config.MapApplicationConfig
import java.nio.file.Path

fun databaseConfig(directory: Path) = MapApplicationConfig(
    "dozy.database.path" to directory.resolve("dozy.db").toString(),
    "dozy.database.busyTimeoutMillis" to "5000",
)
