package com.dozy.domain.id

import java.util.UUID

private fun uuid(
    raw: String,
    version: Int,
): String? {
    if (raw.length != 36) return null
    val uuid =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            return null
        }
    val canonical = uuid.toString()
    return canonical.takeIf { uuid.version() == version && uuid.variant() == 2 && it.equals(raw, ignoreCase = true) }
}

private fun prefixedUuid(
    raw: String?,
    prefix: String,
    version: Int,
): String? {
    val marker = "$prefix-"
    if (raw == null || !raw.startsWith(marker)) return null
    return uuid(raw.substring(marker.length), version)?.let { marker + it }
}

@JvmInline
value class OwnerId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = OwnerId("owner-${Uuids.v7()}")

        fun parse(raw: String?): OwnerId? = prefixedUuid(raw, "owner", 7)?.let(::OwnerId)
    }
}

@JvmInline
value class EmailSessionId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = EmailSessionId("sess-${Uuids.v7()}")

        fun parse(raw: String?): EmailSessionId? = prefixedUuid(raw, "sess", 7)?.let(::EmailSessionId)
    }
}

@JvmInline
value class RefreshToken private constructor(
    val value: String,
) {
    companion object {
        fun generate() = RefreshToken("ref-${Uuids.v4()}")

        fun parse(raw: String?): RefreshToken? = prefixedUuid(raw, "ref", 4)?.takeIf { it == raw }?.let(::RefreshToken)
    }
}

@JvmInline
value class CharacterId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = CharacterId("char-${Uuids.v7()}")

        fun parse(raw: String?): CharacterId? = prefixedUuid(raw, "char", 7)?.let(::CharacterId)
    }
}

@JvmInline
value class PlaceId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = PlaceId("place-${Uuids.v7()}")

        fun parse(raw: String?): PlaceId? = prefixedUuid(raw, "place", 7)?.let(::PlaceId)
    }
}

@JvmInline
value class AssetId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = AssetId("asset-${Uuids.v7()}")

        fun parse(raw: String?): AssetId? = prefixedUuid(raw, "asset", 7)?.let(::AssetId)
    }
}

@JvmInline
value class AvatarJobId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = AvatarJobId("avjob-${Uuids.v7()}")

        fun parse(raw: String?): AvatarJobId? = prefixedUuid(raw, "avjob", 7)?.let(::AvatarJobId)
    }
}

@JvmInline
value class JobId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = JobId("job-${Uuids.v7()}")

        fun parse(raw: String?): JobId? = prefixedUuid(raw, "job", 7)?.let(::JobId)
    }
}

@JvmInline
value class GenerationId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = GenerationId("genid-${Uuids.v7()}")

        fun parse(raw: String?): GenerationId? = prefixedUuid(raw, "genid", 7)?.let(::GenerationId)
    }
}

@JvmInline
value class StoryId private constructor(
    val value: String,
) {
    companion object {
        fun generate() = StoryId("story-${Uuids.v7()}")

        fun parse(raw: String?): StoryId? = prefixedUuid(raw, "story", 7)?.let(::StoryId)
    }
}

@JvmInline
value class LeaseToken private constructor(
    val value: String,
) {
    companion object {
        fun generate() = LeaseToken("lease-${Uuids.v4()}")

        fun parse(raw: String?): LeaseToken? = prefixedUuid(raw, "lease", 4)?.let(::LeaseToken)
    }
}

@JvmInline
value class IdempotencyKey private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String?): IdempotencyKey? = raw?.let { uuid(it, 4) }?.let(::IdempotencyKey)
    }
}
