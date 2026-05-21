package com.kurostream.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * Kuro Stream Application class.
 * 
 * Initializes Hilt dependency injection and configures app-wide settings
 * optimized for low-end Fire TV devices with 1GB RAM.
 */
@HiltAndroidApp
class KuroStreamApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        instance = this
        configureMemoryOptimizations()
    }

    /**
     * Configure memory optimizations for 1GB RAM devices.
     * This includes aggressive garbage collection hints and
     * reduced cache sizes.
     */
    private fun configureMemoryOptimizations() {
        // Request garbage collection on app start to free memory
        System.gc()
    }

    /**
     * Create a memory-optimized ImageLoader for Coil.
     * 
     * Settings are tuned for 1GB RAM devices:
     * - Limited memory cache (50MB max)
     * - Moderate disk cache (100MB)
     * - Aggressive caching policies
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // Use only 15% of available memory
                    .strongReferencesEnabled(false)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(200)
            .respectCacheHeaders(true)
            .build()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Clear image caches when system is low on memory
        imageLoader.memoryCache?.clear()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_UI_HIDDEN,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                imageLoader.memoryCache?.clear()
            }
        }
    }

    companion object {
        @Volatile
        private lateinit var instance: KuroStreamApp

        fun getInstance(): KuroStreamApp = instance
    }
}
