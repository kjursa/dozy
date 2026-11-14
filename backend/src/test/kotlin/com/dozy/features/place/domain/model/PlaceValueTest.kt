package com.dozy.features.place.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaceValueTest {
    @Test
    fun `parses and normalizes place name`() {
        val result = Place.Name.parse("  Woods  ")
        assertEquals("Woods", assertIs<ParseResult.Valid<Place.Name>>(result).value.value)
    }

    @Test
    fun `reports name errors precisely`() {
        assertEquals(Place.Name.Error.Empty, invalidName("  "))
        assertEquals(Place.Name.Error.TooLong(80), invalidName("n".repeat(81)))
        assertEquals(Place.Name.Error.TooLong(80), invalidName("😀".repeat(41)))
        assertIs<ParseResult.Valid<Place.Name>>(Place.Name.parse("😀".repeat(40)))
    }

    @Test
    fun `parses description and reports empty or too long values`() {
        assertEquals("Forest", assertIs<ParseResult.Valid<Place.Description>>(
            Place.Description.parse(" Forest "),
        ).value.value)
        assertEquals(Place.Description.Error.Empty, invalidDescription("\n"))
        assertEquals(Place.Description.Error.TooLong(500), invalidDescription("d".repeat(501)))
    }

    @Test
    fun `parses optional inhabitants and normalizes blank values to null`() {
        val empty = Place.Inhabitants.parse("  ")
        assertEquals(null, assertIs<ParseResult.Valid<Place.Inhabitants?>>(empty).value)
        assertEquals("Owls", assertIs<ParseResult.Valid<Place.Inhabitants?>>(
            Place.Inhabitants.parse(" Owls "),
        ).value?.value)
        assertEquals(Place.Inhabitants.Error.TooLong(500), invalidInhabitants("i".repeat(501)))
    }

    @Test
    fun `aggregates validated fields into save command and returns typed field errors`() {
        val command = assertIs<ParseResult.Valid<SavePlace>>(
            SavePlace.parse(" Woods ", " Quiet forest ", " "),
        ).value
        assertEquals("Woods", command.name.value)
        assertEquals("Quiet forest", command.description.value)
        assertEquals(null, command.inhabitants)
        assertEquals(SavePlace.Error.Name(Place.Name.Error.TooLong(80)), saveError("n".repeat(81), "Forest", null))
        assertEquals(SavePlace.Error.Description(Place.Description.Error.Empty), saveError("Woods", " ", null))
        assertEquals(SavePlace.Error.Inhabitants(Place.Inhabitants.Error.TooLong(500)),
            saveError("Woods", "Forest", "i".repeat(501)))
    }

    private fun invalidName(raw: String) = assertIs<ParseResult.Invalid<Place.Name.Error>>(
        Place.Name.parse(raw),
    ).error

    private fun invalidDescription(raw: String) = assertIs<ParseResult.Invalid<Place.Description.Error>>(
        Place.Description.parse(raw),
    ).error

    private fun invalidInhabitants(raw: String) = assertIs<ParseResult.Invalid<Place.Inhabitants.Error>>(
        Place.Inhabitants.parse(raw),
    ).error

    private fun saveError(name: String, description: String, inhabitants: String?) =
        assertIs<ParseResult.Invalid<SavePlace.Error>>(SavePlace.parse(name, description, inhabitants)).error
}
