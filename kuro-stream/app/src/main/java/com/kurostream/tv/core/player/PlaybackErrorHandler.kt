package com.kurostream.tv.core.player

import androidx.media3.common.PlaybackException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classifies playback errors and recommends a recovery strategy.
 *
 * Recovery priority:
 *  1. Network transient → exponential-back-off retry (max 3 attempts)
 *  2. Source unavailable (4xx / 5xx / not-found) → try next stream
 *  3. Decoder / container unsupported → switch player (ExoPlayer ↔ VLC)
 *  4. DRM → try next stream (DRM is not supported)
 *  5. Timeout → retry once, then switch player
 *  6. Unknown → retry up to [MAX_RETRY_ATTEMPTS], then fatal
 *
 * Error counts are keyed by error category so retries don't accumulate across
 * different error classes. Call [resetErrorCounts] between episodes.
 */
@Singleton
class PlaybackErrorHandler @Inject constructor() {

    companion object {
        private const val TAG = "PlaybackErrorHandler"

        private const val MAX_RETRY_ATTEMPTS          = 3
        private const val INITIAL_RETRY_DELAY_MS      = 1_000L
        private const val MAX_RETRY_DELAY_MS          = 10_000L
        private const val RETRY_BACKOFF_MULTIPLIER    = 2.0
    }

    private val errorCounts    = mutableMapOf<String, Int>()
    private val lastErrorTimes = mutableMapOf<String, Long>()

    // ─── Generic error handler ────────────────────────────────────────────────

    /**
     * Determine the recovery action for an arbitrary [Throwable].
     * Prefer [handleExoPlayerError] for [PlaybackException] instances.
     */
    fun handleError(error: Throwable): RecoveryAction {
        Timber.tag(TAG).e(error, "Handling playback error")
        val key   = errorKey(error)
        val count = (errorCounts.getOrDefault(key, 0) + 1).also { errorCounts[key] = it }
        lastErrorTimes[key] = System.currentTimeMillis()

        return when {
            isNetworkError(error) -> if (count <= MAX_RETRY_ATTEMPTS) {
                val delay = retryDelay(count)
                Timber.tag(TAG).d("Network error — retry in ${delay}ms (attempt $count)")
                RecoveryAction.Retry(delay)
            } else {
                Timber.tag(TAG).d("Max retries for network error — trying next stream")
                RecoveryAction.TryNextStream
            }

            isSourceError(error) -> {
                Timber.tag(TAG).d("Source error — trying next stream")
                RecoveryAction.TryNextStream
            }

            isDecoderError(error) -> {
                Timber.tag(TAG).d("Decoder error — switching player")
                RecoveryAction.SwitchPlayer
            }

            isDrmError(error) -> {
                Timber.tag(TAG).d("DRM error — trying next stream")
                RecoveryAction.TryNextStream
            }

            isTimeoutError(error) -> if (count <= 1) {
                RecoveryAction.Retry(INITIAL_RETRY_DELAY_MS)
            } else {
                RecoveryAction.SwitchPlayer
            }

            else -> if (count <= MAX_RETRY_ATTEMPTS) {
                RecoveryAction.Retry(retryDelay(count))
            } else {
                RecoveryAction.Fatal("Playback failed: ${error.message}")
            }
        }
    }

    // ─── ExoPlayer-specific handler ───────────────────────────────────────────

    /**
     * Determine recovery for a Media3 [PlaybackException].
     */
    fun handleExoPlayerError(error: PlaybackException): RecoveryAction {
        Timber.tag(TAG).e(error, "ExoPlayer error code=${error.errorCode}")
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                val key   = "network_${error.errorCode}"
                val count = (errorCounts.getOrDefault(key, 0) + 1).also { errorCounts[key] = it }
                if (count <= MAX_RETRY_ATTEMPTS) RecoveryAction.Retry(retryDelay(count))
                else RecoveryAction.TryNextStream
            }

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                RecoveryAction.TryNextStream

            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED ->
                RecoveryAction.SwitchPlayer

            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> {
                val count = (errorCounts.getOrDefault("audio", 0) + 1).also { errorCounts["audio"] = it }
                if (count <= 2) RecoveryAction.Retry(500L) else RecoveryAction.SwitchPlayer
            }

            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
            PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
            PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED,
            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
            PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
            PlaybackException.ERROR_CODE_DRM_UNSPECIFIED ->
                RecoveryAction.TryNextStream

            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                RecoveryAction.Retry(0L)

            PlaybackException.ERROR_CODE_TIMEOUT ->
                RecoveryAction.Retry(retryDelay(1))

            PlaybackException.ERROR_CODE_UNSPECIFIED -> {
                val count = (errorCounts.getOrDefault("unspecified", 0) + 1)
                    .also { errorCounts["unspecified"] = it }
                if (count <= MAX_RETRY_ATTEMPTS) RecoveryAction.Retry(retryDelay(count))
                else RecoveryAction.Fatal("Playback failed")
            }

            else -> RecoveryAction.Fatal("Unknown error code: ${error.errorCode}")
        }
    }

    // ─── Session management ───────────────────────────────────────────────────

    /** Call between episodes to clear accumulated retry counts. */
    fun resetErrorCounts() {
        errorCounts.clear()
        lastErrorTimes.clear()
    }

    /** Remove error records older than 5 minutes to prevent stale counts. */
    fun cleanupOldErrors() {
        val cutoff = System.currentTimeMillis() - 5 * 60_000L
        val staleKeys = lastErrorTimes.entries.filter { it.value < cutoff }.map { it.key }
        staleKeys.forEach { key -> errorCounts.remove(key); lastErrorTimes.remove(key) }
    }

    // ─── Error classification ─────────────────────────────────────────────────

    private fun isNetworkError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return e is java.net.UnknownHostException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.SocketException ||
            (e is java.io.IOException && (msg.contains("network") ||
                msg.contains("connection") || msg.contains("unreachable")))
    }

    private fun isSourceError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("404") || msg.contains("not found") ||
            msg.contains("forbidden") || msg.contains("unavailable") ||
            msg.contains("response code: 4") || msg.contains("response code: 5")
    }

    private fun isDecoderError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("decoder") || msg.contains("codec") ||
            msg.contains("format") || msg.contains("unsupported")
    }

    private fun isDrmError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("drm") || msg.contains("widevine") ||
            msg.contains("license") || msg.contains("encrypted")
    }

    private fun isTimeoutError(e: Throwable): Boolean =
        e is java.net.SocketTimeoutException ||
            e.message?.lowercase()?.contains("timeout") == true

    private fun errorKey(e: Throwable): String = when {
        isNetworkError(e) -> "network"
        isSourceError(e)  -> "source"
        isDecoderError(e) -> "decoder"
        isDrmError(e)     -> "drm"
        isTimeoutError(e) -> "timeout"
        else              -> "unknown_${e.javaClass.simpleName}"
    }

    private fun retryDelay(attempt: Int): Long =
        minOf(
            (INITIAL_RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, (attempt - 1).toDouble())).toLong(),
            MAX_RETRY_DELAY_MS
        )

    // ─── Recovery actions ─────────────────────────────────────────────────────

    sealed class RecoveryAction {
        /** Retry the current stream after [delayMs] milliseconds. */
        data class Retry(val delayMs: Long) : RecoveryAction()
        /** Switch to the alternate player (ExoPlayer ↔ VLC). */
        object SwitchPlayer : RecoveryAction()
        /** Move to the next available stream source. */
        object TryNextStream : RecoveryAction()
        /** Unrecoverable — surface [message] to the user. */
        data class Fatal(val message: String) : RecoveryAction()
    }
}

// ─── User-facing error messages ───────────────────────────────────────────────

object PlaybackErrorMessages {

    fun getDisplayMessage(error: Throwable): String {
        val msg = error.message?.lowercase() ?: ""
        return when {
            msg.contains("network") || msg.contains("connection") ->
                "Network error. Please check your internet connection."
            msg.contains("not found") || msg.contains("404") ->
                "Video not available. Please try another source."
            msg.contains("forbidden") || msg.contains("403") ->
                "Access denied. Please try another source."
            msg.contains("timeout") ->
                "Connection timed out. Please try again."
            msg.contains("decoder") || msg.contains("codec") ->
                "Video format not supported. Trying alternate player…"
            msg.contains("drm") || msg.contains("encrypted") ->
                "Protected content. Please try another source."
            msg.contains("bandwidth") || msg.contains("buffer") ->
                "Slow connection. Video may buffer."
            else ->
                "Playback error. Please try again."
        }
    }

    fun getDisplayMessageForExoError(errorCode: Int): String = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED  -> "Network connection failed"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Connection timed out"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS            -> "Server error"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND             -> "Video not found"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED               -> "Video format error"
        PlaybackException.ERROR_CODE_TIMEOUT                       -> "Loading timeout"
        else                                                        -> "Playback error"
    }
}
