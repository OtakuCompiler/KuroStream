package com.kurostream.data.cache

import com.kurostream.cache.CacheNamespaceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManagerImpl @Inject constructor(
    private val namespaceManager: CacheNamespaceManager
) : CacheManager {
    override val artwork: com.kurostream.cache.CacheManager get() = namespaceManager.artwork
    override val metadata: com.kurostream.cache.CacheManager get() = namespaceManager.metadata
    override val subtitles: com.kurostream.cache.CacheManager get() = namespaceManager.subtitles
    override val searchResults: com.kurostream.cache.CacheManager get() = namespaceManager.searchResults
    override val userData: com.kurostream.cache.CacheManager get() = namespaceManager.userData
    override val plugin: com.kurostream.cache.CacheManager get() = namespaceManager.plugin
}
