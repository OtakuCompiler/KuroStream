package com.kurostream.app.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Tracks playback position, saves watch progress periodically,
 * and supports resume/bookmark operations.
 * Allocation-minimal — uses a single coroutine for periodic save.
 */
class PlaybackTracker(
    private val scope: CoroutineScope,
    private val onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
) {
    private var saveJob: Job? = null
    private var _lastPositionMs: Long = 0L
    private var _durationMs: Long = 0L
    private var _isPlaying: Boolean = false

    private val _bookmarkMs = MutableStateFlow<Long?>(null)
    val bookmarkMs: StateFlow<Long?> = _bookmarkMs.asStateFlow()

    private val _abRepeatA = MutableStateFlow<Long?>(null)
    private val _abRepeatB = MutableStateFlow<Long?>(null)
    val abRepeatA: StateFlow<Long?> = _abRepeatA.asStateFlow()
    val abRepeatB: StateFlow<Long?> = _abRepeatB.asStateFlow()

    val lastPositionMs: Long get() = _lastPositionMs

    fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        _isPlaying = playWhenReady && playbackState == Player.STATE_READY

        if (_isPlaying) {
            startAutoSave()
        } else {
            stopAutoSave()
        }

        if (playbackState == Player.STATE_ENDED) {
            onSaveProgress(_durationMs, _durationMs)
        }
    }

    fun onPositionChanged(positionMs: Long, durationMs: Long) {
        _lastPositionMs = positionMs
        _durationMs = durationMs
    }

    /** Jump to last saved position for resume. */
    fun getResumePosition(): Long = _lastPositionMs

    fun setBookmark() {
        _bookmarkMs.value = _lastPositionMs
        Timber.d("PlaybackTracker: bookmark set at ${_lastPositionMs}ms")
    }

    fun clearBookmark() {
        _bookmarkMs.value = null
    }

    fun setABRepeat(a: Long, b: Long) {
        _abRepeatA.value = a
        _abRepeatB.value = b
    }

    fun clearABRepeat() {
        _abRepeatA.value = null
        _abRepeatB.value = null
    }

    private fun startAutoSave() {
        if (saveJob?.isActive == true) return
        saveJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5_000L)
                if (_lastPositionMs > 0 && _lastPositionMs < _durationMs - 10_000) {
                    onSaveProgress(_lastPositionMs, _durationMs)
                }
            }
        }
    }

    private fun stopAutoSave() {
        saveJob?.cancel()
        saveJob = null
    }

    fun destroy() {
        stopAutoSave()
        if (_lastPositionMs > 0) {
            onSaveProgress(_lastPositionMs, _durationMs)
        }
    }
}
