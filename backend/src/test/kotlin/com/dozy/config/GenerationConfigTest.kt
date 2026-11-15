package com.dozy.config

import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GenerationConfigTest {
    @Test fun `typed modes bounds and explicit HTTP opt in`() {
        assertEquals(GenerationConfig(), GenerationConfig.from(MapApplicationConfig()))
        assertFailsWith<IllegalArgumentException> { GenerationConfig(assetBaseUrl = "http://localhost:8080") }
        assertEquals("http://localhost:8080", GenerationConfig(assetBaseUrl = "http://localhost:8080", allowHttp = true).assetBaseUrl)
        assertFailsWith<IllegalArgumentException> { GenerationConfig(assetBaseUrl = "https://example.test/?secret=value") }
        assertFailsWith<IllegalArgumentException> { GenerationConfig(concurrency = 0) }
        assertFailsWith<IllegalArgumentException> { GenerationConfig(delayMillis = -1) }
        assertFailsWith<IllegalArgumentException> { GenerationConfig.from(MapApplicationConfig("dozy.generation.textMode" to "UNKNOWN")) }
    }
}
