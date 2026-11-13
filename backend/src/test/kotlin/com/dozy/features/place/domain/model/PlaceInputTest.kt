package com.dozy.features.place.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlaceInputTest {
    @Test
    fun `normalizes user input and empty inhabitants`() {
        val input = assertNotNull(PlaceInput.from("  Woods  ", "  Quiet forest  ", "  "))
        assertEquals("Woods", input.name)
        assertEquals("Quiet forest", input.description)
        assertNull(input.inhabitants)
        assertEquals("Owls", PlaceInput.from("Woods", "Forest", " Owls ")?.inhabitants)
    }

    @Test
    fun `accepts exact limits after trimming`() {
        assertNotNull(PlaceInput.from(" ${"n".repeat(80)} ", "d".repeat(500), "i".repeat(500)))
        assertNotNull(PlaceInput.from("😀".repeat(40), "Forest", null))
    }

    @Test
    fun `rejects empty required fields and values over UTF16 limits`() {
        assertNull(PlaceInput.from(" ", "Forest", null))
        assertNull(PlaceInput.from("Woods", "\n", null))
        assertNull(PlaceInput.from("n".repeat(81), "Forest", null))
        assertNull(PlaceInput.from("😀".repeat(41), "Forest", null))
        assertNull(PlaceInput.from("Woods", "d".repeat(501), null))
        assertNull(PlaceInput.from("Woods", "Forest", "i".repeat(501)))
    }
}
