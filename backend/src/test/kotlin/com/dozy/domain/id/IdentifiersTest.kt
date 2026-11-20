package com.dozy.domain.id

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentifiersTest {
    @Test
    fun generatedIdentifiersRoundTripWithTheirOwnPrefixAndUuidVersion() {
        assertFormat("owner", 7, OwnerId.generate().value) { OwnerId.parse(it)?.value }
        assertFormat("sess", 7, EmailSessionId.generate().value) { EmailSessionId.parse(it)?.value }
        assertFormat("ref", 4, RefreshToken.generate().value) { RefreshToken.parse(it)?.value }
        assertFormat("char", 7, CharacterId.generate().value) { CharacterId.parse(it)?.value }
        assertFormat("place", 7, PlaceId.generate().value) { PlaceId.parse(it)?.value }
        assertFormat("asset", 7, AssetId.generate().value) { AssetId.parse(it)?.value }
        assertFormat("avjob", 7, AvatarJobId.generate().value) { AvatarJobId.parse(it)?.value }
        assertFormat("job", 7, JobId.generate().value) { JobId.parse(it)?.value }
        assertFormat("genid", 7, GenerationId.generate().value) { GenerationId.parse(it)?.value }
        assertFormat("story", 7, StoryId.generate().value) { StoryId.parse(it)?.value }
        assertFormat("lease", 4, LeaseToken.generate().value) { LeaseToken.parse(it)?.value }
    }

    @Test
    fun validatesPrefixVersionVariantAndCanonicalUuidSpelling() {
        assertNull(JobId.parse(AvatarJobId.generate().value))
        assertNull(AssetId.parse(CharacterId.generate().value))
        assertNull(AssetId.parse("asset-${UUID.randomUUID()}"))
        assertNull(AssetId.parse("asset-019a0000-0000-7000-4000-000000000001"))
        assertNull(AssetId.parse("asset-../something.png"))
        assertNull(AssetId.parse("asset-1-1-7000-8000-1"))
        val normalized = assertNotNull(CharacterId.parse("char-019A0000-0000-7000-8000-000000000001"))
        assertEquals("char-019a0000-0000-7000-8000-000000000001", normalized.value)
    }

    @Test
    fun placeIdsRequireTheSameFormatForSystemAndUserPlaces() {
        val systemId = "place-00000000-0000-7000-8000-000000000001"
        assertEquals(systemId, PlaceId.parse(systemId)?.value)
        assertNull(PlaceId.parse("builtin:woods"))
        assertNull(PlaceId.parse("place-${UUID.randomUUID()}"))
    }

    @Test
    fun preservesClientGeneratedKeys() {
        val key = UUID.randomUUID().toString()
        assertEquals(key, IdempotencyKey.parse(key)?.value)
        assertNull(IdempotencyKey.parse("key-$key"))
    }

    private fun assertFormat(
        prefix: String,
        version: Int,
        value: String,
        parse: (String) -> String?,
    ) {
        assertTrue(value.startsWith("$prefix-"))
        val uuid = value.substringAfter('-')
        assertEquals(version, UUID.fromString(uuid).version())
        assertEquals(value, parse(value))
        assertNull(parse(uuid))
    }
}
