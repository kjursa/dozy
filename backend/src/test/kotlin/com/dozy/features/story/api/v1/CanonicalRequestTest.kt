package com.dozy.features.story.api.v1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.Json

class CanonicalRequestTest {
    private fun canonical(value: String) = canonicalJson(Json.parseToJsonElement(value))
    @Test fun `object order is canonical recursively but arrays and null omission retain meaning`() {
        assertEquals(canonical("""{"cast":[{"id":"one","avatar":{"url":"url","id":"asset"}}],"mode":"MANUAL"}"""),
            canonical("""{"mode":"MANUAL","cast":[{"avatar":{"id":"asset","url":"url"},"id":"one"}]}"""))
        assertNotEquals(canonical("""{"cast":["one","two"]}"""), canonical("""{"cast":["two","one"]}"""))
        assertNotEquals(canonical("""{"idea":null}"""), canonical("{}"))
    }
}
