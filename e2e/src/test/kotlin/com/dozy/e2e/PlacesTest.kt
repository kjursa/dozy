package com.dozy.e2e

import com.dozy.api.client.DozyClient
import com.dozy.api.client.DozyClientConfig
import com.dozy.api.client.auth.v1.AnonymousResult
import com.dozy.api.client.place.v1.PlacesClient
import com.dozy.api.client.place.v1.PlacesCreateResult
import com.dozy.api.client.place.v1.PlacesListResult
import com.dozy.api.client.place.v1.PlacesSaveResult
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacesTest {
    private lateinit var http: HttpClient
    private lateinit var sdk: DozyClient
    private val places get() = sdk.places.v1

    @BeforeTest
    fun setUp() = runBlocking {
        http = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 2_000
            }
        }
        sdk = client()
    }

    @AfterTest
    fun tearDown() {
        if (::sdk.isInitialized) sdk.close()
        http.close()
    }

    @Test
    fun `fresh guest lists shared non-editable system places`() = runBlocking {
        val snapshot = places.snapshot()
        assertTrue(snapshot.isNotEmpty())
        assertTrue(snapshot.all { it.origin == "SYSTEM" && !it.editable })
        assertEquals(snapshot.size, snapshot.map { it.id }.toSet().size)
    }

    @Test
    fun `save normalizes fields and repeated replacement keeps one editable place`() = runBlocking {
        val request = SavePlaceRequest("  Moon garden  ", "  A quiet garden  ", "  Owls  ")
        val id = assertIs<PlacesCreateResult.Success>(places.create(request)).response.id
        assertTrue(id.startsWith("place-"))
        assertEquals(7, UUID.fromString(id.removePrefix("place-")).version())
        val created = places.snapshot().single { it.id == id }
        assertEquals("Moon garden", created.name)
        assertEquals("A quiet garden", created.description)
        assertEquals("Owls", created.inhabitants)
        assertEquals("USER", created.origin)
        assertTrue(created.editable)

        val update = SavePlaceRequest("Star garden", "A different adventure")
        repeat(2) { assertEquals(PlacesSaveResult.Success, places.save(id, update)) }
        val updated = places.snapshot().single { it.id == id }
        assertEquals(update.name, updated.name)
        assertEquals(update.description, updated.description)
        assertNull(updated.inhabitants)
    }

    @Test
    fun `system places reject edits without changing their data`() = runBlocking {
        val original = places.snapshot().first { it.origin == "SYSTEM" }
        assertEquals(
            PlacesSaveResult.ApiError(PlacesSaveError.Forbidden),
            places.save(original.id, SavePlaceRequest("Changed", "Changed description")),
        )
        assertEquals(original, places.snapshot().single { it.id == original.id })
    }

    @Test
    fun `invalid fields return shared errors and never create a record`() = runBlocking {
        val valid = SavePlaceRequest("Garden", "Quiet garden")
        val cases = listOf(
            valid.copy(name = " ") to PlacesSaveError.NameRequired,
            valid.copy(name = "n".repeat(81)) to PlacesSaveError.NameTooLong,
            valid.copy(description = " ") to PlacesSaveError.DescriptionRequired,
            valid.copy(description = "d".repeat(501)) to PlacesSaveError.DescriptionTooLong,
            valid.copy(inhabitants = "i".repeat(501)) to PlacesSaveError.InhabitantsTooLong,
        )
        for ((request, expected) in cases) {
            assertEquals(PlacesCreateResult.ApiError(expected), places.create(request))
            assertTrue(places.snapshot().none { it.origin == "USER" })
        }
    }

    @Test
    fun `another guest cannot see or replace an owned place`() = runBlocking {
        val request = SavePlaceRequest("Our garden", "Our quiet place")
        val id = assertIs<PlacesCreateResult.Success>(places.create(request)).response.id
        val original = places.snapshot().single { it.id == id }
        client().use { other ->
            assertFalse(other.places.v1.snapshot().any { it.id == id })
            assertEquals(
                PlacesSaveResult.ApiError(PlacesSaveError.Conflict),
                other.places.v1.save(id, SavePlaceRequest("Other garden", "Other description")),
            )
        }
        assertEquals(original, places.snapshot().single { it.id == id })
    }

    private suspend fun client(): DozyClient {
        val client = DozyClient(http, DozyClientConfig(System.getenv("DOZY_E2E_BASE_URL") ?: "http://127.0.0.1:8080"))
        try {
            assertIs<AnonymousResult.Success>(client.auth.v1.anonymous(), "Expected a new guest session")
            return client
        } catch (failure: Throwable) {
            client.close()
            throw failure
        }
    }

    private suspend fun PlacesClient.snapshot(): List<PlaceResponse> =
        assertIs<PlacesListResult.Success>(list(), "Expected a successful Places response").response.places
}
