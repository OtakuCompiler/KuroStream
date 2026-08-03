package com.kurostream.common.util

import java.util.concurrent.ConcurrentHashMap

object StringInterner {
    private const val MAX_CACHE_SIZE = 1000
    private val cache = ConcurrentHashMap<String, String>()

    fun intern(str: String?): String? {
        if (str == null) return null
        if (cache.size >= MAX_CACHE_SIZE) {
            val toRemove = cache.keys.take(cache.size / 10)
            toRemove.forEach { cache.remove(it) }
        }
        return cache.computeIfAbsent(str) { it }
    }

    fun internAll(strings: Iterable<String>): List<String> {
        return strings.map { intern(it) ?: it }
    }

    fun internTitle(providerId: String, title: String): String {
        return "$providerId:$title"
    }

    fun internMetadata(name: String): String {
        return intern(name) ?: name
    }

    fun preloadCommonStrings() {
        val common = listOf("Loading", "Error", "Retry", "Cancel", "OK", "Back", "Settings")
        common.forEach { intern(it) }
    }

    fun clear() {
        cache.clear()
    }
}
