package com.dozy.features.place.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaceValueTest {
    @Test
    fun `parses and normalizes place name`() {
        val result = SavePlace.parse("  Woods  ", "Forest", null)
        assertEquals("Woods", assertIs<ParseResult.Valid<SavePlace>>(result).value.name.value)
    }

    @Test
    fun `save command reports name errors precisely while name value accepts older longer data`() {
        assertEquals(SavePlace.Error.Name.Required, saveError("  ", "Forest", null))
        assertEquals(SavePlace.Error.Name.TooLong, saveError("n".repeat(81), "Forest", null))
        assertEquals(SavePlace.Error.Name.TooLong, saveError("😀".repeat(41), "Forest", null))
        assertIs<ParseResult.Valid<SavePlace>>(SavePlace.parse("😀".repeat(40), "Forest", null))
        assertEquals("n".repeat(100), Place.Name("n".repeat(100)).value)
    }

    @Test
    fun `save command reports description errors precisely`() {
        assertEquals("Forest", validSavePlace("Woods", " Forest ", null).description.value)
        assertEquals(SavePlace.Error.Description.Required, saveError("Woods", "\n", null))
        assertEquals(SavePlace.Error.Description.TooLong, saveError("Woods", "d".repeat(501), null))
    }

    @Test
    fun `save command normalizes optional inhabitants and reports excess length`() {
        assertEquals(null, validSavePlace("Woods", "Forest", "  ").inhabitants)
        assertEquals("Owls", validSavePlace("Woods", "Forest", " Owls ").inhabitants?.value)
        assertEquals(SavePlace.Error.Inhabitants.TooLong, saveError("Woods", "Forest", "i".repeat(501)))
    }

    @Test
    fun `aggregates validated fields into save command and returns typed field errors`() {
        val command = assertIs<ParseResult.Valid<SavePlace>>(
            SavePlace.parse(" Woods ", " Quiet forest ", " "),
        ).value
        assertEquals("Woods", command.name.value)
        assertEquals("Quiet forest", command.description.value)
        assertEquals(null, command.inhabitants)
        assertEquals(SavePlace.Error.Name.TooLong, saveError("n".repeat(81), "Forest", null))
        assertEquals(SavePlace.Error.Description.Required, saveError("Woods", " ", null))
        assertEquals(SavePlace.Error.Inhabitants.TooLong,
            saveError("Woods", "Forest", "i".repeat(501)))
    }

    private fun saveError(name: String, description: String, inhabitants: String?) =
        assertIs<ParseResult.Invalid<SavePlace.Error>>(SavePlace.parse(name, description, inhabitants)).error

    private fun validSavePlace(name: String, description: String, inhabitants: String?) =
        assertIs<ParseResult.Valid<SavePlace>>(SavePlace.parse(name, description, inhabitants)).value
}
