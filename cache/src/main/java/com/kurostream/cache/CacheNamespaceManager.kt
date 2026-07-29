package com.kurostream.cache

interface CacheNamespaceManager {
    val searchResults: CacheNamespace
    val metadata: CacheNamespace
}

interface CacheNamespace {
    fun <T> get(key: String): T?
    fun put(key: String, value: Any?, ttlMillis: Long)
}
