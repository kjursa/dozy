package com.dozy.config

import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GenerationConfigTest {
    @Test fun `localhost uses HTTP by default and non-local HTTP requires rejection`() {
        assertEquals(GenerationConfig(), GenerationConfig.from(MapApplicationConfig()))
        assertEquals("http://localhost:8080", GenerationConfig().assetBaseUrl)
        assertFailsWith<IllegalArgumentException> { GenerationConfig(assetBaseUrl = "http://example.test", allowHttp = true) }
        assertFailsWith<IllegalArgumentException> { GenerationConfig(assetBaseUrl = "https://example.test/?secret=value") }
        assertFailsWith<IllegalArgumentException> { GenerationConfig(concurrency = 0) }
        assertFailsWith<IllegalArgumentException> { GenerationConfig(delayMillis = -1) }
        assertFailsWith<IllegalArgumentException> { GenerationConfig.from(MapApplicationConfig("dozy.generation.textMode" to "UNKNOWN")) }
    }
}
