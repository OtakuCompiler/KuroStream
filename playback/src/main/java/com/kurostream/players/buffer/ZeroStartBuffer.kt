package com.kurostream.players.buffer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZeroStartBuffer @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _bufferState = MutableStateFlow<BufferState>(BufferState.Idle)
    val bufferState: StateFlow<BufferState> = _bufferState.asStateFlow()

    sealed class BufferState {
        object Idle : BufferState()
        object Prefetching : BufferState()
        data class Ready(val prebuffer: ByteArray) : BufferState()
        object Failed : BufferState()
    }

    private var currentJob: Job? = null

    fun prefetch(url: String, bytesNeeded: Int = 512 * 1024) {
        cancel()
        currentJob = scope.launch {
            _bufferState.value = BufferState.Prefetching
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-${bytesNeeded - 1}")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("Prefetch failed: ${response.code}")

                val bytes = response.body?.bytes() ?: throw Exception("Empty body")
                _bufferState.value = BufferState.Ready(bytes)

                Timber.i("Zero-start buffer ready: ${bytes.size} bytes")
            } catch (e: Exception) {
                Timber.e(e, "Zero-start prefetch failed")
                _bufferState.value = BufferState.Failed
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        _bufferState.value = BufferState.Idle
    }

    fun consumePrebuffer(): ByteArray? {
        return when (val state = _bufferState.value) {
            is BufferState.Ready -> {
                _bufferState.value = BufferState.Idle
                state.prebuffer
            }
            else -> null
        }
    }
}
