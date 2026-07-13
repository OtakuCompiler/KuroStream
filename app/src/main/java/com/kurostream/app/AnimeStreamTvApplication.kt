// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.transform.Transformation
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import com.kurostream.app.lifecycle.LeakDetector
import com.kurostream.app.lifecycle.StrictModeDebug
import android.os.StrictMode

@HiltAndroidApp
class AnimeStreamTvApplication : Application(), ImageLoaderFactory, ComponentCallbacks2 {

    private var isInBackground = false
    private var memoryMonitor: com.kurostream.common.memory.UnifiedMemoryManager? = null
    private var batteryAwareManager: com.kurostream.common.optimization.BatteryAwareManager? = null
    private var startupProfiler: com.kurostream.common.optimization.StartupProfiler? = null
    
    // Managed coroutine scope for application-level coroutines
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        startupProfiler = com.kurostream.common.optimization.StartupProfiler()
        startupProfiler?.markProcessStart()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
            
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            
            LeakDetector.enable()
        }

        // Initialize memory monitor early
        memoryMonitor = com.kurostream.common.memory.UnifiedMemoryManager.getInstance(this)

        // Initialize battery-aware manager for scheduling decisions
        batteryAwareManager = com.kurostream.common.optimization.BatteryAwareManager.create(this)

        // Register for lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                isInBackground = false
                startupProfiler?.markFirstDraw()
            }

            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                isInBackground = true
                trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            }
        })

        // Startup performance monitoring
        monitorStartupPerformance()
        
        // Pre-intern common strings after first frame
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.os.Looper.myQueue().addIdleHandler {
                com.kurostream.common.util.StringInterner.preloadCommonStrings()
                false
            }
        } else {
            com.kurostream.common.util.StringInterner.preloadCommonStrings()
        }

        // Report fully drawn
        startupProfiler?.markFullyDrawn()

        // Add thermal-aware UI rendering callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerActivityLifecycleCallbacks(@Suppress("EmptyFunctionBlock") object : android.app.Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) = Unit
                override fun onActivityStarted(activity: android.app.Activity) = Unit
                override fun onActivityResumed(activity: android.app.Activity) {
                    com.kurostream.common.thermal.ThermalGuard.getInstance(this@AnimeStreamTvApplication).startMonitoring()
                }
                override fun onActivityPaused(activity: android.app.Activity) = Unit
                override fun onActivityStopped(activity: android.app.Activity) {
                    com.kurostream.common.thermal.ThermalGuard.getInstance(this@AnimeStreamTvApplication).stopMonitoring()
                }
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) = Unit
                override fun onActivityDestroyed(activity: android.app.Activity) = Unit
            })
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Timber.d("TRIM_MEMORY_UI_HIDDEN - clearing caches")
                clearImageCaches()
                com.kurostream.common.pool.BufferPool.shrinkAll()
                com.kurostream.common.util.StringInterner.clear()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Timber.d("TRIM_MEMORY_RUNNING_LOW - reducing memory")
                reduceMemoryFootprint()
                com.kurostream.common.pool.BufferPool.shrinkAll()
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                Timber.d("TRIM_MEMORY_BACKGROUND - aggressive cleanup")
                aggressiveMemoryCleanup()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Timber.d("TRIM_MEMORY_MODERATE/COMPLETE - moderate cleanup")
                moderateMemoryCleanup()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle config changes without recreating
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("onLowMemory - emergency cleanup")
        aggressiveMemoryCleanup()
    }

    private fun clearImageCaches() {
        // Clear Coil memory cache
        try {
            val imageLoader = ImageLoader.get(this)
            imageLoader.memoryCache.clear()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear Coil memory cache")
        }
    }

    private fun reduceMemoryFootprint() {
        clearImageCaches()
        // Reduce playback buffer
        try {
            // PlaybackManager.getInstance()?.reduceBuffer()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reduce playback buffer")
        }
        
        // Clear object pools
        try {
            com.kurostream.common.pool.ObjectPools.clearAll()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear object pools")
        }
    }

    private fun moderateMemoryCleanup() {
        reduceMemoryFootprint()
        // Clear Coil disk cache if needed
        try {
            val imageLoader = ImageLoader.get(this)
            appScope.launch(Dispatchers.IO) {
                imageLoader.diskCache.clear()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear Coil disk cache")
        }
    }

    private fun aggressiveMemoryCleanup() {
        moderateMemoryCleanup()
        // Force GC hint (suppressed - explicit GC calls discouraged)
        
        // Clear string interner
        com.kurostream.common.util.StringInterner.clear()
        
        // Clear buffer pools
        try {
            com.kurostream.common.pool.BufferPool.clearAll()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear buffer pools")
        }
    }

    private fun monitorStartupPerformance() {
        if (BuildConfig.DEBUG) {
            appScope.launch(Dispatchers.IO) {
                System.currentTimeMillis()
                // Track startup metrics
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.coroutineContext.cancel()
    }

    override fun newImageLoader(): ImageLoader {
        com.kurostream.common.memory.LowRamDevice.initialize(this)
        val memoryCacheSize = com.kurostream.common.memory.LowRamDevice.coilMemoryCacheSize
        val diskCacheSize = com.kurostream.common.memory.LowRamDevice.coilDiskCacheSize
        
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSize(memoryCacheSize.toLong())
                    .weakReferenceEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheSize)
                    .build()
            }
            .defaultRequestOptions {
                // Use RGB_565 for large images to halve memory
                crossfade(true)
                transformations = listOf(
                    Transformation { request, pool ->
                        if (request.data.diskCacheKey?.length ?: 0 > 512 * 1024) {
                            // Large image - use RGB_565
                            request.newBuilder()
                                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                .build()
                        } else {
                            request
                        }
                    }
                )
            }
            .respectCacheHeaders(false)
            .build()
    }
}
