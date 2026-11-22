package com.dozy.domain

/** A null list means the caller already has this version. */
data class VersionedCollection<T>(
    val version: String,
    val items: List<T>?,
)
