package com.kurostream.domain.repository

interface CacheRepository {
    suspend fun <T> getOrFetch(cacheKey: String, cacheTtlMs: Long, fetch: suspend () -> T): T
}
