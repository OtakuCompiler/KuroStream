package com.kurostream.webos.optimization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebOSOptimizations @Inject constructor(
    private val context: Context
) {
    private val TAG = "WebOSOptimizations"
    
    private val _isWebOS = MutableStateFlow(false)
    val isWebOS: StateFlow<Boolean> = _isWebOS.asStateFlow()
    
    private val _webOSVersion = MutableStateFlow(0)
    val webOSVersion: StateFlow<Int> = _webOSVersion.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val progressiveImageLoader = ProgressiveImageLoader()
    private val lazyUIManager = LazyUIManager()
    private val objectPoolManager = ObjectPoolManager()
    private val cacheTrimmer = CacheTrimmer(context)
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val backgroundExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "WebOS-BG").apply { priority = Thread.MIN_PRIORITY }
    }
    
    init {
        detectWebOS()
        applyOptimizations()
    }
    
    private fun detectWebOS() {
        val isWebOS = Build.MANUFACTURER.equals("LG", ignoreCase = true) ||
            Build.BRAND.equals("LG", ignoreCase = true) ||
            Build.DEVICE.contains("webos", ignoreCase = true) ||
            Build.MODEL.contains("webOS", ignoreCase = true) ||
            context.packageManager.hasSystemFeature("com.webos.feature.webos")
        
        _isWebOS.value = isWebOS
        
        if (isWebOS) {
            val version = try {
                Build.VERSION.RELEASE.toIntOrNull() ?: 0
            } catch (e: Exception) {
                0
            }
            _webOSVersion.value = version
            Timber.d("webOS detected: version=$version, model=${Build.MODEL}, device=${Build.DEVICE}")
        }
    }
    
    private fun applyOptimizations() {
        if (!_isWebOS.value) return
        
        lazyUIManager.enable()
        
        objectPoolManager.initializePools()
        
        cacheTrimmer.setPlaybackStateListener { isPlaying ->
            _isPlaying.value = isPlaying
        }
        
        scope.launch {
            progressiveImageLoader.start()
        }
        
        applyWebOSThreadPriorities()
        
        Timber.i("webOS optimizations applied")
    }
    
    private fun applyWebOSThreadPriorities() {
        scope.launch {
            Thread.currentThread().priority = Thread.NORM_PRIORITY - 1
        }
        
        backgroundExecutor.submit {
            Thread.currentThread().priority = Thread.MIN_PRIORITY
        }
    }
    
    fun getProgressiveImageLoader(): ProgressiveImageLoader = progressiveImageLoader
    fun getLazyUIManager(): LazyUIManager = lazyUIManager
    fun getObjectPoolManager(): ObjectPoolManager = objectPoolManager
    fun getCacheTrimmer(): CacheTrimmer = cacheTrimmer
    
    fun onPlaybackStarted() {
        _isPlaying.value = true
        cacheTrimmer.onPlaybackStarted()
        progressiveImageLoader.reduceQuality()
        lazyUIManager.deferNonCriticalUI()
    }
    
    fun onPlaybackStopped() {
        _isPlaying.value = false
        cacheTrimmer.onPlaybackStopped()
        progressiveImageLoader.restoreQuality()
        lazyUIManager.resumeNormalUI()
    }
    
    fun onTrimMemory(level: Int) {
        cacheTrimmer.trim(level)
        objectPoolManager.trim()
        progressiveImageLoader.clearCache()
    }
    
    fun shutdown() {
        scope.cancel()
        backgroundExecutor.shutdown()
        progressiveImageLoader.shutdown()
        lazyUIManager.shutdown()
        objectPoolManager.shutdown()
        cacheTrimmer.shutdown()
    }
}

class ProgressiveImageLoader {
    private val memoryCache = LruCache<String, Bitmap>(32 * 1024 * 1024) { _, bitmap ->
        bitmap.byteCount
    }
    private val diskCacheDir: java.io.File
    private val loadQueue = Channel<ImageLoadRequest>(100)
    private val currentQuality = AtomicInteger(100)
    private val isRunning = AtomicBoolean(false)
    
    data class ImageLoadRequest(
        val url: String,
        val targetWidth: Int,
        val targetHeight: Int,
        val callback: (Bitmap?) -> Unit,
        val priority: Int = 0,
    )
    
    init {
        diskCacheDir = java.io.File(
            android.content.Context.getSystemService(android.content.Context.FILE_INTEGRITY_SERVICE)?.javaClass
                ?.getDeclaredField("cacheDir")?.let { (it.get(null) as? java.io.File)?.absolutePath }
                ?: android.os.Environment.getDataDirectory().absolutePath,
            "kurostream_image_cache"
        )
        diskCacheDir.mkdirs()
    }
    
    fun start() {
        if (isRunning.getAndSet(true)) return
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            while (isRunning.get()) {
                val request = loadQueue.receiveOrNull() ?: continue
                
                val bitmap = loadImageProgressive(request)
                request.callback(bitmap)
            }
        }
    }
    
    private fun loadImageProgressive(request: ImageLoadRequest): Bitmap? {
        val cacheKey = "${request.url}_${request.targetWidth}x${request.targetHeight}"
        
        val cached = memoryCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return cached
        }
        
        val diskFile = java.io.File(diskCacheDir, cacheKey.hashCode().toString())
        if (diskFile.exists()) {
            val bitmap = loadFromDisk(diskFile, request.targetWidth, request.targetHeight)
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
                return bitmap
            }
        }
        
        return downloadAndDecode(request, cacheKey, diskFile)
    }
    
    private fun downloadAndDecode(request: ImageLoadRequest, cacheKey: String, diskFile: java.io.File): Bitmap? {
        return try {
            val quality = currentQuality.get()
            val url = java.net.URL(request.url)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            val inputStream = connection.inputStream
            val byteArrayOutputStream = ByteArrayOutputStream()
            inputStream.copyTo(byteArrayOutputStream)
            val data = byteArrayOutputStream.toByteArray()
            
            diskFile.writeBytes(data)
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            
            val inSampleSize = calculateInSampleSize(options, request.targetWidth, request.targetHeight)
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
            }
            
            bitmap
        } catch (e: Exception) {
            Timber.w(e, "Failed to load image: ${request.url}")
            null
        }
    }
    
    private fun loadFromDisk(file: java.io.File, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            val inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load from disk: ${file.absolutePath}")
            null
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    fun loadImage(url: String, width: Int, height: Int, callback: (Bitmap?) -> Unit) {
        val request = ImageLoadRequest(url, width, height, callback)
        loadQueue.trySend(request)
    }
    
    fun reduceQuality() {
        currentQuality.set(60)
        memoryCache.resize(16 * 1024 * 1024)
    }
    
    fun restoreQuality() {
        currentQuality.set(100)
        memoryCache.resize(32 * 1024 * 1024)
    }
    
    fun clearCache() {
        memoryCache.evictAll()
    }
    
    fun shutdown() {
        isRunning.set(false)
        loadQueue.close()
        memoryCache.evictAll()
    }
}

class LazyUIManager {
    private val deferredComponents = ConcurrentHashMap<String, () -> Unit>()
    private var isDeferredMode = false
    
    fun enable() {
        isDeferredMode = true
    }
    
    fun deferComponent(key: String, component: () -> Unit) {
        if (isDeferredMode) {
            deferredComponents[key] = component
        } else {
            component()
        }
    }
    
    fun deferNonCriticalUI() {
        isDeferredMode = true
    }
    
    fun resumeNormalUI() {
        isDeferredMode = false
        deferredComponents.values.forEach { it() }
        deferredComponents.clear()
    }
    
    @Composable
    fun LazyComponent(
        key: String,
        content: @Composable () -> Unit
    ) {
        val lazyManager = remember { this }
        
        DisposableEffect(key) {
            lazyManager.deferComponent(key) {
            }
            onDispose {
            }
        }
        
        if (!lazyManager.isDeferredMode) {
            content()
        } else {
            androidx.compose.runtime.remember(key) {
                content
            }
        }
    }
    
    fun shutdown() {
        deferredComponents.clear()
    }
}

class ObjectPoolManager {
    private val bitmapPool = ConcurrentHashMap<Int, java.util.concurrent.ConcurrentLinkedQueue<Bitmap>>()
    private val byteBufferPool = ConcurrentHashMap<Int, java.util.concurrent.ConcurrentLinkedQueue<java.nio.ByteBuffer>>()
    private val byteArrayPool = ConcurrentHashMap<Int, java.util.concurrent.ConcurrentLinkedQueue<ByteArray>>()
    private val stringBuilderPool = java.util.concurrent.ConcurrentLinkedQueue<StringBuilder>()
    
    fun initializePools() {
        prefillBitmapPool()
        prefillByteBufferPool()
        prefillByteArrayPool()
    }
    
    private fun prefillBitmapPool() {
        val sizes = intArrayOf(1920 * 1080 * 2, 1280 * 720 * 2, 640 * 360 * 2)
        sizes.forEach { size ->
            val queue = java.util.concurrent.ConcurrentLinkedQueue<Bitmap>()
            repeat(4) {
                val bitmap = Bitmap.createBitmap(
                    kotlin.math.sqrt(size / 2.0).toInt(),
                    kotlin.math.sqrt(size / 2.0).toInt(),
                    Bitmap.Config.RGB_565
                )
                queue.add(bitmap)
            }
            bitmapPool[size] = queue
        }
    }
    
    private fun prefillByteBufferPool() {
        val sizes = intArrayOf(64 * 1024, 256 * 1024, 1024 * 1024, 4 * 1024 * 1024)
        sizes.forEach { size ->
            val queue = java.util.concurrent.ConcurrentLinkedQueue<java.nio.ByteBuffer>()
            repeat(8) {
                queue.add(java.nio.ByteBuffer.allocateDirect(size))
            }
            byteBufferPool[size] = queue
        }
    }
    
    private fun prefillByteArrayPool() {
        val sizes = intArrayOf(4096, 16384, 65536, 262144, 1048576)
        sizes.forEach { size ->
            val queue = java.util.concurrent.ConcurrentLinkedQueue<ByteArray>()
            repeat(16) {
                queue.add(ByteArray(size))
            }
            byteArrayPool[size] = queue
        }
    }
    
    fun acquireBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap? {
        val size = width * height * (if (config == Bitmap.Config.ARGB_8888) 4 else 2)
        val queue = bitmapPool[size]
        val bitmap = queue?.poll()
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.reconfigure(width, height, config)
            bitmap.eraseColor(0)
            return bitmap
        }
        return try {
            Bitmap.createBitmap(width, height, config)
        } catch (e: OutOfMemoryError) {
            null
        }
    }
    
    fun releaseBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        val size = bitmap.byteCount
        val queue = bitmapPool.getOrPut(size) { java.util.concurrent.ConcurrentLinkedQueue() }
        if (queue.size < 8) {
            queue.add(bitmap)
        }
    }
    
    fun acquireByteBuffer(size: Int): java.nio.ByteBuffer {
        val adjustedSize = size.nextPowerOfTwo()
        val queue = byteBufferPool[adjustedSize] ?: java.util.concurrent.ConcurrentLinkedQueue()
        val buffer = queue.poll() ?: java.nio.ByteBuffer.allocateDirect(adjustedSize)
        buffer.clear()
        buffer.limit(size)
        return buffer
    }
    
    fun releaseByteBuffer(buffer: java.nio.ByteBuffer) {
        val size = buffer.capacity()
        val queue = byteBufferPool.getOrPut(size) { java.util.concurrent.ConcurrentLinkedQueue() }
        if (queue.size < 16) {
            buffer.clear()
            queue.add(buffer)
        }
    }
    
    fun acquireByteArray(size: Int): ByteArray {
        val adjustedSize = size.nextPowerOfTwo()
        val queue = byteArrayPool[adjustedSize] ?: java.util.concurrent.ConcurrentLinkedQueue()
        return queue.poll() ?: ByteArray(adjustedSize)
    }
    
    fun releaseByteArray(array: ByteArray) {
        val size = array.size
        val queue = byteArrayPool.getOrPut(size) { java.util.concurrent.ConcurrentLinkedQueue() }
        if (queue.size < 32) {
            queue.add(array)
        }
    }
    
    fun acquireStringBuilder(): StringBuilder {
        return stringBuilderPool.poll()?.apply { setLength(0) } ?: StringBuilder(128)
    }
    
    fun releaseStringBuilder(sb: StringBuilder) {
        if (sb.capacity() <= 1024 && stringBuilderPool.size < 32) {
            stringBuilderPool.add(sb)
        }
    }
    
    fun trim() {
        bitmapPool.values.forEach { it.clear() }
        byteBufferPool.values.forEach { it.clear() }
        byteArrayPool.values.forEach { it.clear() }
        stringBuilderPool.clear()
    }
    
    fun shutdown() {
        bitmapPool.values.forEach { queue ->
            queue.forEach { it.recycle() }
            queue.clear()
        }
        bitmapPool.clear()
        byteBufferPool.clear()
        byteArrayPool.clear()
        stringBuilderPool.clear()
    }
}

private fun <K, V> ConcurrentHashMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return getOrPutIfAbsent(key, defaultValue)
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

class CacheTrimmer(
    private val context: Context
) {
    private var isPlaying = false
    private var playbackStateListener: ((Boolean) -> Unit)? = null
    private val trimThresholds = mapOf(
        android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN to 0.3f,
        android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE to 0.5f,
        android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW to 0.7f,
        android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL to 0.9f,
        android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND to 0.5f,
    )
    
    fun setPlaybackStateListener(listener: (Boolean) -> Unit) {
        playbackStateListener = listener
    }
    
    fun onPlaybackStarted() {
        isPlaying = true
        trimCaches(0.5f)
    }
    
    fun onPlaybackStopped() {
        isPlaying = false
        trimCaches(0.2f)
    }
    
    fun trim(level: Int) {
        val threshold = trimThresholds[level] ?: 0.3f
        trimCaches(threshold)
    }
    
    private fun trimCaches(ratio: Float) {
        val targetRatio = if (isPlaying) ratio * 1.5f else ratio
        
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
        context.externalCacheDir?.deleteRecursively()
        context.externalCacheDir?.mkdirs()
        
        val databasesDir = java.io.File(context.filesDir.parent, "databases")
        if (databasesDir.exists()) {
            databasesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith("-wal") || file.name.endsWith("-shm")) {
                    file.delete()
                }
            }
        }
        
        Timber.d("Cache trimmed with ratio: $targetRatio (playing: $isPlaying)")
    }
    
    fun shutdown() {
        playbackStateListener = null
    }
}

class WebOSLifecycleObserver(
    private val optimizations: WebOSOptimizations
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        if (optimizations.isWebOS.value) {
            optimizations.lazyUIManager.enable()
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
    }
    
    override fun onPause(owner: LifecycleOwner) {
    }
    
    override fun onStop(owner: LifecycleOwner) {
        optimizations.lazyUIManager.resumeNormalUI()
        optimizations.objectPoolManager.trim()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        optimizations.shutdown()
    }
}

@Composable
fun WebOSProgressiveImage(
    url: String,
    width: Int,
    height: Int,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    contentDescription: String? = null,
) {
    val progressiveLoader = remember { WebOSOptimizations(context = androidx.compose.ui.platform.LocalContext.current).progressiveImageLoader }
    val bitmapState = androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null)
    
    androidx.compose.runtime.LaunchedEffect(url, width, height) {
        progressiveLoader.loadImage(url, width, height) { bitmap ->
            bitmapState.value = bitmap
        }
    }
    
    bitmapState.value?.let { bitmap ->
        androidx.compose.ui.graphics.ImageBitmap.imageResource(bitmap).also { imageBitmap ->
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.graphics.painter.PainterResources.imageBitmap(imageBitmap),
                contentDescription = contentDescription ?: "",
                modifier = modifier.size(width.dp, height.dp)
            )
        }
    } ?: {
        androidx.compose.foundation.Box(
            modifier = modifier
                .size(width.dp, height.dp)
                .background(androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f))
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = androidx.compose.ui.Alignment.Center.align(modifier.size(24.dp))
            )
        }
    }
}