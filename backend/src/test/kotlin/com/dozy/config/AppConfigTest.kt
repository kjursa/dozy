package com.dozy.config

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.yaml.YamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AppConfigTest {
    @Test
    fun `loads bundled YAML and environment defaults`() {
        val yaml = assertNotNull(YamlConfig("application.yaml"))
        val config = AppConfig.from(yaml)
        assertEquals(System.getenv("DOZY_DATABASE_PATH") ?: "data/dozy.db", config.database.path)
        assertEquals((System.getenv("DOZY_DATABASE_BUSY_TIMEOUT_MS") ?: "5000").toInt(), config.database.busyTimeoutMillis)
        assertEquals(System.getenv("PORT") ?: "8080", yaml.property("ktor.deployment.port").getString())
    }

    @Test
    fun `loads explicit values and rejects unsafe configuration`() {
        val config = AppConfig.from(MapApplicationConfig(
            "dozy.database.path" to "/tmp/a directory/dozy.db",
            "dozy.database.busyTimeoutMillis" to "1234",
        ))
        assertEquals(DatabaseConfig("/tmp/a directory/dozy.db", 1234), config.database)
        assertFailsWith<IllegalArgumentException> { DatabaseConfig(" ", 5000) }
        assertFailsWith<IllegalArgumentException> { DatabaseConfig(":memory:", 5000) }
        assertFailsWith<IllegalArgumentException> { DatabaseConfig("dozy.db", 0) }
        assertFailsWith<IllegalArgumentException> { DatabaseConfig("dozy.db", 60001) }
        assertFailsWith<NumberFormatException> {
            AppConfig.from(MapApplicationConfig("dozy.database.path" to "dozy.db",
                "dozy.database.busyTimeoutMillis" to "invalid"))
        }
    }
}
