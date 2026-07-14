package com.kurostream.cache.tier

import android.content.Context
import android.os.Environment
import android.util.Log
import com.kurostream.cache.CacheManager
import com.kurostream.cache.CacheStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TieredCacheManager @Inject constructor(
    private val context: Context,
) {

    private val TAG = "TieredCacheManager"

    data class CacheTierConfig(
        val internalCacheMaxMb: Int = 200,
        val externalCacheMaxMb: Int = 500,
        val promotionThreshold: Int = 3,
        val demotionThresholdDays: Int = 7,
    )

    data class CacheItem(
        val key: String,
        val size: Long,
        val accessCount: Int,
        val lastAccessTime: Long,
        val tier: CacheTier,
    )

    enum class CacheTier { INTERNAL, EXTERNAL, NONE }

    private val itemRegistry = ConcurrentHashMap<String, CacheItem>()
    private val accessCounts = ConcurrentHashMap<String, AtomicLong>()
    private val config = CacheTierConfig()

    private val internalCacheDir: File by lazy {
        File(context.cacheDir, "tiered_internal").apply { mkdirs() }
    }

    private val externalCacheDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_CACHE), "tiered_external").apply { mkdirs() }
    }

    private val _stats = MutableStateFlow(TieredStats())
    val stats: Flow<TieredStats> = _stats.asStateFlow()

    data class TieredStats(
        val internalItemCount: Int = 0,
        val externalItemCount: Int = 0,
        val internalSizeMb: Long = 0,
        val externalSizeMb: Long = 0,
        val promotions: Long = 0,
        val demotions: Long = 0,
    )

    fun recordAccess(key: String, size: Long) {
        val count = accessCounts.getOrPut(key) { AtomicLong(0) }
        val newCount = count.incrementAndGet()

        val existing = itemRegistry[key]
        if (existing != null) {
            itemRegistry[key] = existing.copy(
                accessCount = newCount.toInt(),
                lastAccessTime = System.currentTimeMillis(),
            )

            if (newCount >= config.promotionThreshold && existing.tier == CacheTier.EXTERNAL) {
                promoteToInternal(key)
            }
        } else {
            val tier = if (internalCacheDir.usableSpace > size) CacheTier.INTERNAL else CacheTier.EXTERNAL
            itemRegistry[key] = CacheItem(
                key = key,
                size = size,
                accessCount = newCount.toInt(),
                lastAccessTime = System.currentTimeMillis(),
                tier = tier,
            )
            // Create zero-byte marker file so the entry has a disk presence
            getCachePath(key)?.createNewFile()
        }

        persistRegistry()
        updateStats()
    }

    private fun persistRegistry() {
        try {
            val registryFile = File(internalCacheDir.parentFile, "tier_registry.json")
            val json = com.google.gson.Gson().toJson(itemRegistry.entries.toList())
            registryFile.writeText(json)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist registry", e)
        }
    }

    fun getCachePath(key: String): File? {
        val item = itemRegistry[key] ?: return null
        return when (item.tier) {
            CacheTier.INTERNAL -> File(internalCacheDir, sanitizeKey(key))
            CacheTier.EXTERNAL -> File(externalCacheDir, sanitizeKey(key))
            CacheTier.NONE -> null
        }
    }

    fun promoteToInternal(key: String) {
        val item = itemRegistry[key] ?: return
        if (item.tier == CacheTier.INTERNAL) return

        try {
            val source = getCacheFile(item.copy(tier = CacheTier.EXTERNAL))
            val dest = getCacheFile(item.copy(tier = CacheTier.INTERNAL))

            if (source != null && source.exists() && dest != null) {
                source.copyTo(dest, overwrite = true)
                itemRegistry[key] = item.copy(tier = CacheTier.INTERNAL)
                Log.d(TAG, "Promoted $key to internal cache")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to promote $key to internal", e)
        }
    }

    fun demoteToExternal(key: String) {
        val item = itemRegistry[key] ?: return
        if (item.tier == CacheTier.EXTERNAL) return

        try {
            val source = getCacheFile(item.copy(tier = CacheTier.INTERNAL))
            val dest = getCacheFile(item.copy(tier = CacheTier.EXTERNAL))

            if (source != null && source.exists() && dest != null) {
                source.copyTo(dest, overwrite = true)
                source.delete()
                itemRegistry[key] = item.copy(tier = CacheTier.EXTERNAL)
                Log.d(TAG, "Demoted $key to external cache")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to demote $key to external", e)
        }
    }

    fun cleanupStaleEntries() {
        val now = System.currentTimeMillis()
        val maxAge = config.demotionThresholdDays * 24 * 60 * 60 * 1000L

        itemRegistry.values
            .filter { now - it.lastAccessTime > maxAge && it.tier == CacheTier.INTERNAL }
            .forEach { item ->
                if (item.accessCount < config.promotionThreshold) {
                    demoteToExternal(item.key)
                }
            }

        updateStats()
    }

    private fun getCacheFile(item: CacheItem): File? {
        val dir = when (item.tier) {
            CacheTier.INTERNAL -> internalCacheDir
            CacheTier.EXTERNAL -> externalCacheDir
            CacheTier.NONE -> return null
        }
        return File(dir, sanitizeKey(item.key))
    }

    private fun sanitizeKey(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(200)
    }

    private fun updateStats() {
        val items = itemRegistry.values
        _stats.value = TieredStats(
            internalItemCount = items.count { it.tier == CacheTier.INTERNAL },
            externalItemCount = items.count { it.tier == CacheTier.EXTERNAL },
            internalSizeMb = items.filter { it.tier == CacheTier.INTERNAL }.sumOf { it.size } / 1024 / 1024,
            externalSizeMb = items.filter { it.tier == CacheTier.EXTERNAL }.sumOf { it.size } / 1024 / 1024,
        )
    }
}
