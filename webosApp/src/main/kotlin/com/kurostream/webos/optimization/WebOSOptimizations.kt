package com.kurostream.webos.optimization

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebOSOptimizations @Inject constructor(
    private val context: Context,
    private val detector: WebOSDetector = WebOSDetector(context),
    private val imageCacheManager: ImageCacheManager = ImageCacheManager(),
    private val bufferPoolManager: BufferPoolManager = BufferPoolManager(),
    private val cacheTrimmer: CacheTrimmer = CacheTrimmer(context),
) {
    val isWebOS: Boolean get() = detector.isWebOS
    val webOSVersion: Int get() = detector.webOSVersion

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        if (detector.isWebOS) {
            bufferPoolManager.initializePools()
            cacheTrimmer.setPlaybackStateListener { isPlaying ->
                _isPlaying.value = isPlaying
            }
            Timber.i("webOS optimizations applied (v${detector.webOSVersion})")
        }
    }

    fun getProgressiveImageLoader(): ImageCacheManager = imageCacheManager

    fun onPlaybackStarted() {
        _isPlaying.value = true
        cacheTrimmer.onPlaybackStarted()
        imageCacheManager.reduceQuality()
    }

    fun onPlaybackStopped() {
        _isPlaying.value = false
        cacheTrimmer.onPlaybackStopped()
        imageCacheManager.restoreQuality()
    }

    fun onTrimMemory(level: Int) {
        cacheTrimmer.trim(level)
        bufferPoolManager.trim()
        imageCacheManager.clearCache()
    }

    fun shutdown() {
        scope.cancel()
        imageCacheManager.shutdown()
        bufferPoolManager.shutdown()
        cacheTrimmer.shutdown()
    }
}

class WebOSDetector(private val context: Context) {
    val isWebOS: Boolean
    val webOSVersion: Int

    init {
        val detected = Build.MANUFACTURER.equals("LG", ignoreCase = true) ||
            Build.BRAND.equals("LG", ignoreCase = true) ||
            Build.DEVICE.contains("webos", ignoreCase = true) ||
            Build.MODEL.contains("webOS", ignoreCase = true) ||
            context.packageManager.hasSystemFeature("com.webos.feature.webos")

        isWebOS = detected
        webOSVersion = if (detected) {
            Build.VERSION.RELEASE.toIntOrNull() ?: 0
        } else 0

        if (detected) {
            Timber.d("webOS detected: version=$webOSVersion, model=${Build.MODEL}")
        }
    }
}

class ImageCacheManager {
    private val memoryCache = LruCache<String, Bitmap>(32 * 1024 * 1024) { _, bitmap ->
        bitmap.byteCount
    }
    private var diskCacheDir: java.io.File? = null
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

    fun initDiskCache(context: Context) {
        if (diskCacheDir != null) return
        diskCacheDir = java.io.File(context.cacheDir, "kurostream_image_cache").also { it.mkdirs() }
    }

    fun start() {
        if (isRunning.getAndSet(true)) return
        if (diskCacheDir == null) return

        CoroutineScope(Dispatchers.IO).launch {
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
        if (cached != null && !cached.isRecycled) return cached

        val dir = diskCacheDir ?: return downloadAndDecode(request, cacheKey, null)
        val diskFile = java.io.File(dir, cacheKey.hashCode().toString())
        if (diskFile.exists()) {
            val bitmap = loadFromDisk(diskFile, request.targetWidth, request.targetHeight)
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
                return bitmap
            }
        }
        return downloadAndDecode(request, cacheKey, diskFile)
    }

    private fun downloadAndDecode(request: ImageLoadRequest, cacheKey: String, diskFile: java.io.File?): Bitmap? {
        return try {
            val url = java.net.URL(request.url)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            val inputStream = connection.inputStream
            val byteArrayOutputStream = ByteArrayOutputStream()
            inputStream.copyTo(byteArrayOutputStream)
            val data = byteArrayOutputStream.toByteArray()

            diskFile?.writeBytes(data)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            val inSampleSize = calculateInSampleSize(options, request.targetWidth, request.targetHeight)
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            if (bitmap != null) memoryCache.put(cacheKey, bitmap)
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
        loadQueue.trySend(ImageLoadRequest(url, width, height, callback))
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
            onDispose { }
        }

        if (!lazyManager.isDeferredMode) {
            content()
        }
    }

    fun shutdown() {
        deferredComponents.clear()
    }
}

class BufferPoolManager {
    private val bitmapPool = ConcurrentHashMap<Int, ConcurrentLinkedQueue<Bitmap>>()
    private val byteBufferPool = ConcurrentHashMap<Int, ConcurrentLinkedQueue<java.nio.ByteBuffer>>()
    private val byteArrayPool = ConcurrentHashMap<Int, ConcurrentLinkedQueue<ByteArray>>()
    private val stringBuilderPool = ConcurrentLinkedQueue<StringBuilder>()

    fun initializePools() {
        prefillBitmapPool()
        prefillByteBufferPool()
        prefillByteArrayPool()
    }

    private fun prefillBitmapPool() {
        val sizes = intArrayOf(1920 * 1080 * 2, 1280 * 720 * 2, 640 * 360 * 2)
        sizes.forEach { size ->
            val queue = ConcurrentLinkedQueue<Bitmap>()
            repeat(4) {
                val bitmap = Bitmap.createBitmap(
                    kotlin.math.sqrt(size / 2.0).toInt().coerceAtLeast(1),
                    kotlin.math.sqrt(size / 2.0).toInt().coerceAtLeast(1),
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
            val queue = ConcurrentLinkedQueue<java.nio.ByteBuffer>()
            repeat(8) {
                queue.add(java.nio.ByteBuffer.allocateDirect(size))
            }
            byteBufferPool[size] = queue
        }
    }

    private fun prefillByteArrayPool() {
        val sizes = intArrayOf(4096, 16384, 65536, 262144, 1048576)
        sizes.forEach { size ->
            val queue = ConcurrentLinkedQueue<ByteArray>()
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
        val queue = bitmapPool.getOrPut(size) { ConcurrentLinkedQueue() }
        if (queue.size < 8) {
            queue.add(bitmap)
        }
    }

    fun acquireByteBuffer(size: Int): java.nio.ByteBuffer {
        val adjustedSize = size.nextPowerOfTwo()
        val queue = byteBufferPool[adjustedSize] ?: ConcurrentLinkedQueue()
        val buffer = queue.poll() ?: java.nio.ByteBuffer.allocateDirect(adjustedSize)
        buffer.clear()
        buffer.limit(size)
        return buffer
    }

    fun releaseByteBuffer(buffer: java.nio.ByteBuffer) {
        val size = buffer.capacity()
        val queue = byteBufferPool.getOrPut(size) { ConcurrentLinkedQueue() }
        if (queue.size < 16) {
            buffer.clear()
            queue.add(buffer)
        }
    }

    fun acquireByteArray(size: Int): ByteArray {
        val adjustedSize = size.nextPowerOfTwo()
        val queue = byteArrayPool[adjustedSize] ?: ConcurrentLinkedQueue()
        return queue.poll() ?: ByteArray(adjustedSize)
    }

    fun releaseByteArray(array: ByteArray) {
        val size = array.size
        val queue = byteArrayPool.getOrPut(size) { ConcurrentLinkedQueue() }
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
        bitmapPool.values.forEach { queue ->
            queue.forEach { if (!it.isRecycled) it.recycle() }
            queue.clear()
        }
        byteBufferPool.values.forEach { it.clear() }
        byteArrayPool.values.forEach { it.clear() }
        stringBuilderPool.clear()
    }

    fun shutdown() {
        bitmapPool.values.forEach { queue ->
            queue.forEach { if (!it.isRecycled) it.recycle() }
            queue.clear()
        }
        bitmapPool.clear()
        byteBufferPool.clear()
        byteArrayPool.clear()
        stringBuilderPool.clear()
    }
}

class CacheTrimmer(
    private val context: Context
) {
    private var isPlaying = false
    private var playbackStateListener: ((Boolean) -> Unit)? = null
    private val trimThresholds = mapOf(
        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN to 0.3f,
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE to 0.5f,
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW to 0.7f,
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL to 0.9f,
        ComponentCallbacks2.TRIM_MEMORY_BACKGROUND to 0.5f,
    )

    private val databaseLock = Any()

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

        synchronized(databaseLock) {
            val databasesDir = java.io.File(context.filesDir.parent, "databases")
            if (databasesDir.exists()) {
                val walShmFiles = databasesDir.listFiles()?.filter {
                    it.name.endsWith("-wal") || it.name.endsWith("-shm")
                } ?: emptyList()
                if (walShmFiles.isNotEmpty()) {
                    Timber.d("Found ${walShmFiles.size} WAL/SHM files to clean up")
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
        if (optimizations.isWebOS) {
            Timber.d("WebOSLifecycleObserver: onStart")
        }
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
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    optimizations: WebOSOptimizations,
) {
    val imageCacheManager = remember { optimizations.getProgressiveImageLoader() }
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(url, width, height) {
        imageCacheManager.loadImage(url, width, height) { bitmap ->
            bitmapState.value = bitmap
        }
    }

    bitmapState.value?.let { bitmap ->
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        androidx.compose.foundation.Image(
            painter = BitmapPainter(imageBitmap),
            contentDescription = contentDescription ?: "",
            modifier = modifier.size(width.dp, height.dp)
        )
    } ?: Box(
        modifier = modifier
            .size(width.dp, height.dp)
            .background(Color.Gray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp)
        )
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
