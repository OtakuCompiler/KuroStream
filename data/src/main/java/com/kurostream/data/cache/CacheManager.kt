package com.kurostream.data.cache

import com.kurostream.cache.CacheNamespaceManager

interface CacheManager {
    val artwork: com.kurostream.cache.CacheManager
    val metadata: com.kurostream.cache.CacheManager
    val subtitles: com.kurostream.cache.CacheManager
    val searchResults: com.kurostream.cache.CacheManager
    val userData: com.kurostream.cache.CacheManager
    val plugin: com.kurostream.cache.CacheManager
}
