package com.kurostream.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class KuroCacheManager @Inject constructor(
    private val context: Context,
) : CacheNamespaceManager {

    private val rootDir = File(context.cacheDir, "kurostream")
    private val memoryCache = ConcurrentHashMap<String, ByteArray>()
    private val maxMemoryBytes = 8 * 1024 * 1024
    private var currentMemoryBytes = 0

    private val ramDiskDir: File? by lazy {
        val tmpfs = File("/data/local/tmp/kurostream_ramdisk")
        if (tmpfs.exists() || tmpfs.mkdirs()) {
            return@lazy tmpfs
        }
        context.cacheDir?.let { cache ->
            File(cache, "ramdisk").apply { mkdirs() }
        }
    }

    private val vodDiskDir: File by lazy {
        File(context.cacheDir, "vod_500mb").apply { mkdirs() }
    }

    override suspend fun put(key: String, value: ByteArray) = withContext(Dispatchers.IO) {
        val vodFile = File(vodDiskDir, key)
        vodFile.parentFile?.mkdirs()
        vodFile.writeBytes(value)
        
        if (value.size <= 64 * 1024 && currentMemoryBytes + value.size <= maxMemoryBytes) {
            synchronized(memoryCache) {
                currentMemoryBytes += value.size
                memoryCache[key] = value
            }
        }
        
        ramDiskDir?.let { ram ->
            val ramFile = File(ram, key)
            ramFile.parentFile?.mkdirs()
            kotlin.runCatching { ramFile.writeBytes(value) }
        }
    }

    override suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        memoryCache[key]?.let { return@withContext it }
        
        ramDiskDir?.let { ram ->
            val ramFile = File(ram, key)
            if (ramFile.exists()) {
                return@withContext kotlin.runCatching { ramFile.readBytes() }.getOrNull()
            }
        }
        
        kotlin.runCatching { File(vodDiskDir, key).readBytes() }.getOrNull()
    }

    suspend fun getMapped(key: String): java.nio.ByteBuffer? = withContext(Dispatchers.IO) {
        val file = File(ramDiskDir ?: return@withContext null, key)
        if (!file.exists()) return@withContext null
        try {
            val channel = java.io.RandomAccessFile(file, "r").channel
            channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, file.length())
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun invalidateNamespace(namespace: String) = withContext(Dispatchers.IO) {
        File(vodDiskDir, namespace).deleteRecursively()
        File(ramDiskDir, namespace).deleteRecursively()
        synchronized(memoryCache) {
            memoryCache.keys.removeAll { it.startsWith(namespace) }
        }
    }

    override suspend fun namespaceSizeBytes(namespace: String): Long = withContext(Dispatchers.IO) {
        File(vodDiskDir, namespace).walk().sumOf { it.length() }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        vodDiskDir.deleteRecursively()
        ramDiskDir?.deleteRecursively()
        memoryCache.clear()
        currentMemoryBytes = 0
    }

    suspend fun totalCacheSize(): Long = withContext(Dispatchers.IO) {
        vodDiskDir.walk().sumOf { it.length() }
    }

    suspend fun enforceBudget(maxBytes: Long = 500L * 1024 * 1024) = withContext(Dispatchers.IO) {
        val current = totalCacheSize()
        if (current > maxBytes) {
            val files = vodDiskDir.walk()
                .filter { it.isFile }
                .sortedBy { it.lastModified() }
                .toList()
            var freed = 0L
            for (file in files) {
                if (current - freed <= maxBytes * 0.85) break
                val len = file.length()
                file.delete()
                freed += len
            }
            Timber.d("Cache trim: freed ${freed/1024/1024}MB")
        }
    }
}
