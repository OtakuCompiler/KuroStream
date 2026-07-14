package com.kurostream.cache.internal

internal data class CacheEntrySerializable(
    val value: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val version: Int = 1
) {
    fun isExpired(): Boolean {
        if (expiresAt <= 0) return false
        return System.currentTimeMillis() > expiresAt
    }
}
