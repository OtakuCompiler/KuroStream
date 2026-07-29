package com.kurostream.domain.repository

interface CacheRepository {
    suspend fun <T> getOrFetch(
        key: String,
        ttlMs: Long,
        fetch: suspend () -> T
    ): T

    suspend fun <T> get(key: String): T?

    suspend fun put(key: String, value: Any, ttlMs: Long = 0)

    suspend fun invalidate(key: String)

    suspend fun clearAll()
}
