package com.kurostream.players.performance

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.layer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.kurostream.players.memory.AdaptiveMemoryGovernor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FireTVOptimizations @Inject constructor(
    private val context: Context,
    private val adaptiveMemoryGovernor: AdaptiveMemoryGovernor,
) {
    private val TAG = "FireTVOptimizations"
    
    private val _isFireTV = MutableStateFlow(false)
    val isFireTV: StateFlow<Boolean> = _isFireTV.asStateFlow()
    
    private val _isFireTVStickHD = MutableStateFlow(false)
    val isFireTVStickHD: StateFlow<Boolean> = _isFireTVStickHD.asStateFlow()
    
    private val _networkThrottler = NetworkThrottler(maxConcurrentRequests = 3)
    val networkThrottler: NetworkThrottler = _networkThrottler
    
    private val _imageDecodeLimiter = ImageDecodeLimiter(maxDecodePixels = 1920 * 1080)
    val imageDecodeLimiter: ImageDecodeLimiter = _imageDecodeLimiter
    
    private val _hardwareLayerManager = HardwareLayerManager()
    val hardwareLayerManager: HardwareLayerManager = _hardwareLayerManager
    
    private val _animationReducer = AnimationReducer()
    val animationReducer: AnimationReducer = _animationReducer
    
    private val backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(2, FireTVThreadFactory())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    init {
        detectFireTV()
        applyOptimizations()
    }
    
    private fun detectFireTV() {
        val isAmazon = Build.MANUFACTURER.equals("Amazon", ignoreCase = true) ||
            Build.BRAND.equals("Amazon", ignoreCase = true)
        
        val isFireTVModel = Build.MODEL.contains("AFT", ignoreCase = true) ||
            Build.MODEL.contains("Fire TV", ignoreCase = true) ||
            Build.DEVICE.contains("aft", ignoreCase = true)
        
        val hasLeanback = context.packageManager.hasSystemFeature("android.software.leanback")
        
        val isFireTV = isAmazon || isFireTVModel || hasLeanback
        _isFireTV.value = isFireTV
        
        if (isFireTV) {
            val memInfo = android.app.ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memInfo)
            val totalMemMb = (memInfo.totalMem / (1024 * 1024)).toInt()
            
            val isStickHD = totalMemMb <= 2048 &&
                (Build.MODEL.contains("AFTM", ignoreCase = true) || 
                 Build.MODEL.contains("AFTS", ignoreCase = true) ||
                 totalMemMb <= 1536)
            
            _isFireTVStickHD.value = isStickHD
            
            Timber.d("Fire TV detected: model=${Build.MODEL}, totalMem=${totalMemMb}MB, isStickHD=$isStickHD")
        }
    }
    
    private fun applyOptimizations() {
        if (!_isFireTV.value) return
        
        val config = adaptiveMemoryGovernor.getFireTVStickHDConfig()
        
        networkThrottler.setMaxConcurrent(config.maxConcurrentNetworkRequests)
        
        imageDecodeLimiter.setMaxDecodePixels(config.maxImageDecodeSize)
        
        if (config.enableHardwareLayersForLists) {
            hardwareLayerManager.enableForLists()
        }
        
        if (config.reduceAnimationDuration) {
            animationReducer.setDurationScale(0.5f)
        }
        
        if (config.disableComplexAnimations) {
            animationReducer.disableComplexAnimations()
        }
        
        limitBackgroundThreads(config.limitBackgroundThreads)
        
        applyNetworkOptimizations()
        
        Timber.i("Fire TV optimizations applied: config=$config")
    }
    
    private fun applyNetworkOptimizations() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        scope.launch {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: android.net.Network, networkCapabilities: NetworkCapabilities) {
                    val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val hasEthernet = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    val hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    
                    val bandwidth = if (hasWifi || hasEthernet) "high" else if (hasCellular) "medium" else "low"
                    networkThrottler.setBandwidthProfile(bandwidth)
                }
                
                override fun onLost(network: android.net.Network) {
                    networkThrottler.setBandwidthProfile("low")
                }
            }
            
            try {
                val request = android.net.NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            } catch (e: Exception) {
                Timber.w(e, "Failed to register network callback")
            }
        }
    }
    
    private fun limitBackgroundThreads(maxThreads: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        
        ThreadPoolExecutorManager.getInstance().setCorePoolSize(maxThreads)
        ThreadPoolExecutorManager.getInstance().setMaximumPoolSize(maxThreads * 2)
    }
    
    fun getNetworkThrottler(): NetworkThrottler = _networkThrottler
    fun getImageDecodeLimiter(): ImageDecodeLimiter = _imageDecodeLimiter
    fun getHardwareLayerManager(): HardwareLayerManager = _hardwareLayerManager
    fun getAnimationReducer(): AnimationReducer = _animationReducer
    
    fun shutdown() {
        scope.cancel()
        backgroundExecutor.shutdown()
        networkThrottler.shutdown()
        hardwareLayerManager.cleanup()
        animationReducer.reset()
    }
}

class NetworkThrottler(private var maxConcurrentRequests: Int = 3) {
    private val requestChannel = Channel<NetworkRequest>(maxConcurrentRequests * 2)
    private val activeRequests = AtomicInteger(0)
    private var bandwidthProfile = "high"
    
    data class NetworkRequest(
        val id: Long,
        val execute: suspend () -> Unit,
    )
    
    fun setMaxConcurrent(max: Int) {
        maxConcurrentRequests = max.coerceAtLeast(1)
    }
    
    fun setBandwidthProfile(profile: String) {
        bandwidthProfile = profile
        maxConcurrentRequests = when (profile) {
            "high" -> 4
            "medium" -> 3
            "low" -> 2
            else -> 3
        }
    }
    
    suspend fun enqueue(request: suspend () -> Unit): Unit = coroutineScope {
        val id = System.nanoTime()
        requestChannel.send(NetworkRequest(id, request))
        
        while (activeRequests.get() >= maxConcurrentRequests) {
            kotlinx.coroutines.delay(50)
        }
        
        activeRequests.incrementAndGet()
        try {
            request()
        } finally {
            activeRequests.decrementAndGet()
        }
    }
    
    fun shutdown() {
        requestChannel.close()
    }
}

class ImageDecodeLimiter(private var maxDecodePixels: Int = 1920 * 1080) {
    private val decodeSemaphore = kotlinx.coroutines.sync.Semaphore(2)
    
    fun setMaxDecodePixels(maxPixels: Int) {
        maxDecodePixels = maxPixels.coerceAtLeast(640 * 480)
    }
    
    suspend fun decodeWithLimit(
        source: android.graphics.BitmapFactory.Options,
        decodeAction: suspend (android.graphics.BitmapFactory.Options) -> android.graphics.Bitmap?
    ): android.graphics.Bitmap? {
        decodeSemaphore.withPermit {
            val inSampleSize = calculateInSampleSize(source.outWidth, source.outHeight, maxDecodePixels)
            source.inSampleSize = inSampleSize
            source.inJustDecodeBounds = false
            source.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            source.inMutable = true
            
            return@withPermit decodeAction(source)
        }
    }
    
    private fun calculateInSampleSize(width: Int, height: Int, maxPixels: Int): Int {
        var inSampleSize = 1
        if (height > 0 && width > 0) {
            val totalPixels = width.toLong() * height
            if (totalPixels > maxPixels) {
                inSampleSize = Math.max(1, kotlin.math.sqrt(totalPixels.toDouble() / maxPixels).toInt())
            }
        }
        return inSampleSize.nextPowerOfTwo()
    }
    
    fun shutdown() {
        decodeSemaphore.close()
    }
}

private fun Int.nextPowerOfTwo(): Int {
    var v = this - 1
    v = v or (v shr 1)
    v = v or (v shr 2)
    v = v or (v shr 4)
    v = v or (v shr 8)
    v = v or (v shr 16)
    return v + 1
}

class HardwareLayerManager {
    private val hardwareLayerViews = mutableSetOf<View>()
    private var enabledForLists = false
    
    fun enableForLists() {
        enabledForLists = true
    }
    
    fun applyToView(view: View) {
        if (enabledForLists && view.isHardwareAccelerated) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            hardwareLayerViews.add(view)
        }
    }
    
    fun removeView(view: View) {
        view.setLayerType(View.LAYER_TYPE_NONE, null)
        hardwareLayerViews.remove(view)
    }
    
    fun cleanup() {
        hardwareLayerViews.forEach { it.setLayerType(View.LAYER_TYPE_NONE, null) }
        hardwareLayerViews.clear()
    }
}

@Composable
fun HardwareLayerListItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hardwareLayerManager = remember { HardwareLayerManager() }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                this.setLayerType(android.view.View.LAYER_TYPE_HARDWARE)
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

class AnimationReducer {
    private var durationScale = 1.0f
    private var complexAnimationsDisabled = false
    
    fun setDurationScale(scale: Float) {
        durationScale = scale.coerceIn(0.1f, 1.0f)
    }
    
    fun disableComplexAnimations() {
        complexAnimationsDisabled = true
    }
    
    fun enableComplexAnimations() {
        complexAnimationsDisabled = false
    }
    
    fun getDurationScale(): Float = durationScale
    fun isComplexAnimationsDisabled(): Boolean = complexAnimationsDisabled
    
    fun reset() {
        durationScale = 1.0f
        complexAnimationsDisabled = false
    }
    
    @Composable
    fun ReducedAnimation(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        if (complexAnimationsDisabled) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                content()
            }
        } else {
            Box(modifier = modifier
                .graphicsLayer {
                    this.alpha = durationScale
                },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

class FireTVThreadFactory : ThreadFactory {
    private val threadNumber = AtomicInteger(1)
    
    override fun newThread(r: Runnable): Thread {
        return Thread(r, "FireTV-BG-${threadNumber.getAndIncrement()}").apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }
    }
}

object ThreadPoolExecutorManager {
    @Volatile private var INSTANCE: ThreadPoolExecutorManager? = null
    
    fun getInstance(): ThreadPoolExecutorManager = INSTANCE ?: synchronized(this) {
        INSTANCE ?: ThreadPoolExecutorManager().also { INSTANCE = it }
    }
}

class ThreadPoolExecutorManager {
    private var corePoolSize = 4
    private var maximumPoolSize = 8
    
    fun setCorePoolSize(size: Int) {
        corePoolSize = size
    }
    
    fun setMaximumPoolSize(size: Int) {
        maximumPoolSize = size
    }
}

class FireTVLifecycleObserver(
    private val optimizations: FireTVOptimizations
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        if (optimizations.isFireTV.value) {
            optimizations.hardwareLayerManager.enableForLists()
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        optimizations.hardwareLayerManager.cleanup()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        optimizations.shutdown()
    }
}

class NetworkRequest(
    val execute: suspend () -> Unit
)