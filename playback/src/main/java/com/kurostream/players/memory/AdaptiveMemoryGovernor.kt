package com.kurostream.players.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveMemoryGovernor @Inject constructor(
    private val context: Context,
    private val adaptiveMemoryManager: AdaptiveMemoryManager,
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _cachePolicy = MutableStateFlow(CachePolicy())
    val cachePolicy: StateFlow<CachePolicy> = _cachePolicy.asStateFlow()
    
    private val _prefetchPolicy = MutableStateFlow(PrefetchPolicy())
    val prefetchPolicy: StateFlow<PrefetchPolicy> = _prefetchPolicy.asStateFlow()
    
    private val _thermalState = MutableStateFlow(ThermalState.NOMINAL)
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()
    
    private val _deviceProfile = MutableStateFlow(DeviceProfile.UNKNOWN)
    val deviceProfile: StateFlow<DeviceProfile> = _deviceProfile.asStateFlow()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val thermalCallback: android.os.PowerManager.OnThermalStatusChangedListener? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        android.os.PowerManager.OnThermalStatusChangedListener { status ->
            _thermalState.value = when (status) {
                android.os.PowerManager.THERMAL_STATUS_NONE -> ThermalState.NOMINAL
                android.os.PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
                android.os.PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                android.os.PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
                android.os.PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
                android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.EMERGENCY
                android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.SHUTDOWN
                else -> ThermalState.NOMINAL
            }
            updatePolicies()
        }
    } else null
    
    private val memoryStateSubscription = adaptiveMemoryManager.memoryState
        .onEach { updatePolicies() }
        .launchIn(scope)
    
    init {
        detectDeviceProfile()
        registerThermalListener()
        updatePolicies()
        
        handler.postDelayed(periodicUpdate, 30_000)
    }
    
    private fun detectDeviceProfile() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMemMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val isLowRam = activityManager.isLowRamDevice
        val isTV = context.packageManager.hasSystemFeature("android.software.leanback")
        val isFireTV = Build.MANUFACTURER.equals("Amazon", ignoreCase = true) || 
            Build.MODEL.contains("AFT", ignoreCase = true) ||
            Build.MODEL.contains("Fire TV", ignoreCase = true)
        
        _deviceProfile.value = when {
            isFireTV && totalMemMb <= 2048 -> DeviceProfile.FIRE_TV_STICK_HD
            isFireTV && totalMemMb <= 4096 -> DeviceProfile.FIRE_TV_STICK_4K
            isFireTV -> DeviceProfile.FIRE_TV_CUBE
            isTV && totalMemMb <= 2048 -> DeviceProfile.ANDROID_TV_LOW
            isTV && totalMemMb <= 4096 -> DeviceProfile.ANDROID_TV_MID
            isTV -> DeviceProfile.ANDROID_TV_HIGH
            isLowRam -> DeviceProfile.MOBILE_LOW_RAM
            totalMemMb <= 3072 -> DeviceProfile.MOBILE_LOW_RAM
            totalMemMb <= 6144 -> DeviceProfile.MOBILE_MID
            else -> DeviceProfile.MOBILE_HIGH
        }
        
        Timber.d("Device profile detected: ${_deviceProfile.value}, totalMem=${totalMemMb}MB, isTV=$isTV, isFireTV=$isFireTV")
    }
    
    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            try {
                powerManager.addThermalStatusListener(thermalCallback!!)
                Timber.d("Thermal listener registered")
            } catch (e: Exception) {
                Timber.w(e, "Failed to register thermal listener")
            }
        }
    }
    
    private fun updatePolicies() {
        val memoryState = adaptiveMemoryManager.memoryState.value
        val thermal = _thermalState.value
        val profile = _deviceProfile.value
        
        val availableMb = memoryState.availableMemoryMb
        val pressure = memoryState.memoryPressure
        val isCritical = memoryState.isCritical
        
        val newCachePolicy = when {
            isCritical || thermal >= ThermalState.SEVERE -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.25).toInt(),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.25).toInt(),
                metadataCacheSizeMb = (profile.baseMetadataCacheMb * 0.5).toInt(),
                enableImageCompression = true,
                imageQuality = 50,
                evictNonEssential = true,
            )
            pressure > 0.85f || thermal >= ThermalState.MODERATE -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.5).toInt(),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.5).toInt(),
                metadataCacheSizeMb = (profile.baseMetadataCacheMb * 0.75).toInt(),
                enableImageCompression = true,
                imageQuality = 70,
                evictNonEssential = true,
            )
            pressure > 0.6f || thermal >= ThermalState.LIGHT -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.75).toInt(),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.75).toInt(),
                metadataCacheSizeMb = profile.baseMetadataCacheMb,
                enableImageCompression = true,
                imageQuality = 80,
                evictNonEssential = false,
            )
            else -> CachePolicy(
                imageCacheSizeMb = profile.baseImageCacheMb,
                artworkCacheSizeMb = profile.baseArtworkCacheMb,
                metadataCacheSizeMb = profile.baseMetadataCacheMb,
                enableImageCompression = false,
                imageQuality = 90,
                evictNonEssential = false,
            )
        }
        
        val newPrefetchPolicy = when {
            isCritical || thermal >= ThermalState.SEVERE -> PrefetchPolicy(
                enabled = false,
                maxConcurrentRequests = 1,
                prefetchDepth = 0,
                chunkSizeKb = 64,
            )
            pressure > 0.85f || thermal >= ThermalState.MODERATE -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = 2,
                prefetchDepth = 1,
                chunkSizeKb = 256,
            )
            pressure > 0.6f || thermal >= ThermalState.LIGHT -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = 3,
                prefetchDepth = 2,
                chunkSizeKb = 512,
            )
            else -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = profile.maxConcurrentRequests,
                prefetchDepth = profile.basePrefetchDepth,
                chunkSizeKb = 1024,
            )
        }
        
        if (newCachePolicy != _cachePolicy.value) {
            _cachePolicy.value = newCachePolicy
            Timber.d("Cache policy updated: ${newCachePolicy.imageCacheSizeMb}MB images, ${newCachePolicy.artworkCacheSizeMb}MB artwork, quality=${newCachePolicy.imageQuality}")
        }
        
        if (newPrefetchPolicy != _prefetchPolicy.value) {
            _prefetchPolicy.value = newPrefetchPolicy
            Timber.d("Prefetch policy updated: enabled=${newPrefetchPolicy.enabled}, concurrent=${newPrefetchPolicy.maxConcurrentRequests}, depth=${newPrefetchPolicy.prefetchDepth}")
        }
    }
    
    private val periodicUpdate = object : Runnable {
        override fun run() {
            adaptiveMemoryManager.updateMemoryState()
            handler.postDelayed(this, 30_000)
        }
    }
    
    fun applyCachePolicy(imageCache: ImageCache, artworkCache: ArtworkCache, metadataCache: MetadataCache) {
        val policy = _cachePolicy.value
        
        imageCache.resize(policy.imageCacheSizeMb)
        imageCache.setCompressionEnabled(policy.enableImageCompression)
        imageCache.setQuality(policy.imageQuality)
        
        artworkCache.resize(policy.artworkCacheSizeMb)
        artworkCache.setCompressionEnabled(policy.enableImageCompression)
        artworkCache.setQuality(policy.imageQuality)
        
        metadataCache.resize(policy.metadataCacheSizeMb)
        
        if (policy.evictNonEssential) {
            imageCache.evictNonEssential()
            artworkCache.evictNonEssential()
        }
    }
    
    fun applyPrefetchPolicy(prefetchManager: PrefetchManager) {
        val policy = _prefetchPolicy.value
        prefetchManager.setEnabled(policy.enabled)
        prefetchManager.setMaxConcurrentRequests(policy.maxConcurrentRequests)
        prefetchManager.setPrefetchDepth(policy.prefetchDepth)
        prefetchManager.setChunkSizeKb(policy.chunkSizeKb)
    }
    
    fun getFireTVStickHDConfig(): FireTVStickHDConfig {
        return FireTVStickHDConfig(
            maxConcurrentNetworkRequests = 3,
            maxImageDecodeSize = 1920 * 1080,
            enableHardwareLayersForLists = true,
            reduceAnimationDuration = true,
            disableComplexAnimations = true,
            limitBackgroundThreads = 2,
            maxTextureSize = 2048,
        )
    }
    
    fun getWebOSConfig(): WebOSOptimizationConfig {
        return WebOSOptimizationConfig(
            enableProgressiveImageLoading = true,
            enableLazyUIRendering = true,
            enableObjectPooling = true,
            trimCachesDuringPlayback = true,
            playbackCacheTrimRatio = 0.5f,
            maxImageDecodeThreads = 2,
            enableWebOSMemoryPressureCallback = true,
        )
    }
    
    fun shutdown() {
        handler.removeCallbacks(periodicUpdate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && thermalCallback != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            try {
                powerManager.removeThermalStatusListener(thermalCallback!!)
            } catch (e: Exception) {
                Timber.w(e, "Failed to remove thermal listener")
            }
        }
        scope.cancel()
    }
    
    interface ImageCache {
        fun resize(sizeMb: Int)
        fun setCompressionEnabled(enabled: Boolean)
        fun setQuality(quality: Int)
        fun evictNonEssential()
    }
    
    interface ArtworkCache {
        fun resize(sizeMb: Int)
        fun setCompressionEnabled(enabled: Boolean)
        fun setQuality(quality: Int)
        fun evictNonEssential()
    }
    
    interface MetadataCache {
        fun resize(sizeMb: Int)
    }
    
    interface PrefetchManager {
        fun setEnabled(enabled: Boolean)
        fun setMaxConcurrentRequests(max: Int)
        fun setPrefetchDepth(depth: Int)
        fun setChunkSizeKb(kb: Int)
    }
}

data class CachePolicy(
    val imageCacheSizeMb: Int = 100,
    val artworkCacheSizeMb: Int = 50,
    val metadataCacheSizeMb: Int = 20,
    val enableImageCompression: Boolean = false,
    val imageQuality: Int = 90,
    val evictNonEssential: Boolean = false,
)

data class PrefetchPolicy(
    val enabled: Boolean = true,
    val maxConcurrentRequests: Int = 4,
    val prefetchDepth: Int = 3,
    val chunkSizeKb: Int = 1024,
)

enum class ThermalState {
    NOMINAL, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN
}

enum class DeviceProfile {
    UNKNOWN,
    FIRE_TV_STICK_HD,
    FIRE_TV_STICK_4K,
    FIRE_TV_CUBE,
    ANDROID_TV_LOW,
    ANDROID_TV_MID,
    ANDROID_TV_HIGH,
    MOBILE_LOW_RAM,
    MOBILE_MID,
    MOBILE_HIGH,
    
    val baseImageCacheMb: Int
        get() = when (this) {
            FIRE_TV_STICK_HD -> 40
            FIRE_TV_STICK_4K -> 80
            FIRE_TV_CUBE -> 150
            ANDROID_TV_LOW -> 40
            ANDROID_TV_MID -> 80
            ANDROID_TV_HIGH -> 150
            MOBILE_LOW_RAM -> 30
            MOBILE_MID -> 80
            MOBILE_HIGH -> 200
            else -> 60
        }
    
    val baseArtworkCacheMb: Int
        get() = when (this) {
            FIRE_TV_STICK_HD -> 20
            FIRE_TV_STICK_4K -> 40
            FIRE_TV_CUBE -> 80
            ANDROID_TV_LOW -> 20
            ANDROID_TV_MID -> 40
            ANDROID_TV_HIGH -> 80
            MOBILE_LOW_RAM -> 15
            MOBILE_MID -> 40
            MOBILE_HIGH -> 100
            else -> 30
        }
    
    val baseMetadataCacheMb: Int
        get() = when (this) {
            FIRE_TV_STICK_HD -> 10
            FIRE_TV_STICK_4K -> 20
            FIRE_TV_CUBE -> 40
            ANDROID_TV_LOW -> 10
            ANDROID_TV_MID -> 20
            ANDROID_TV_HIGH -> 40
            MOBILE_LOW_RAM -> 5
            MOBILE_MID -> 15
            MOBILE_HIGH -> 30
            else -> 15
        }
    
    val basePrefetchDepth: Int
        get() = when (this) {
            FIRE_TV_STICK_HD -> 1
            FIRE_TV_STICK_4K -> 2
            FIRE_TV_CUBE -> 3
            ANDROID_TV_LOW -> 1
            ANDROID_TV_MID -> 2
            ANDROID_TV_HIGH -> 3
            MOBILE_LOW_RAM -> 1
            MOBILE_MID -> 2
            MOBILE_HIGH -> 3
            else -> 2
        }
    
    val maxConcurrentRequests: Int
        get() = when (this) {
            FIRE_TV_STICK_HD -> 3
            FIRE_TV_STICK_4K -> 4
            FIRE_TV_CUBE -> 6
            ANDROID_TV_LOW -> 3
            ANDROID_TV_MID -> 4
            ANDROID_TV_HIGH -> 6
            MOBILE_LOW_RAM -> 2
            MOBILE_MID -> 4
            MOBILE_HIGH -> 6
            else -> 4
        }
}

data class FireTVStickHDConfig(
    val maxConcurrentNetworkRequests: Int = 3,
    val maxImageDecodeSize: Int = 1920 * 1080,
    val enableHardwareLayersForLists: Boolean = true,
    val reduceAnimationDuration: Boolean = true,
    val disableComplexAnimations: Boolean = true,
    val limitBackgroundThreads: Int = 2,
    val maxTextureSize: Int = 2048,
)

data class WebOSOptimizationConfig(
    val enableProgressiveImageLoading: Boolean = true,
    val enableLazyUIRendering: Boolean = true,
    val enableObjectPooling: Boolean = true,
    val trimCachesDuringPlayback: Boolean = true,
    val playbackCacheTrimRatio: Float = 0.5f,
    val maxImageDecodeThreads: Int = 2,
    val enableWebOSMemoryPressureCallback: Boolean = true,
)