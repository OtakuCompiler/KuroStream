package com.kurostream.tv.core.player

import android.content.Context
import android.media.MediaCodecList
import androidx.media3.exoplayer.ExoPlayer
import com.kurostream.tv.core.player.vlc.VlcPlayerAdapter
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.di.PlayerModule
import com.kurostream.tv.domain.provider.ProviderStream
import com.kurostream.tv.domain.provider.StreamType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart player router that automatically selects between ExoPlayer and VLC
 * based on stream type and device codec support.
 *
 * Decision logic:
 *  1. Torrent / EXTERNAL → VLC (always)
 *  2. Legacy container extensions (.avi, .wmv, …) → VLC
 *  3. Codec reported as unsupported without hardware acceleration → VLC
 *  4. HLS / DASH → ExoPlayer (always)
 *  5. Otherwise → respect [preferExoPlayer] flag (default: ExoPlayer)
 */
@Singleton
class SmartPlayerRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayerFactory: PlayerModule.ExoPlayerFactory,
    private val vlcPlayerAdapter: VlcPlayerAdapter,
    private val playbackErrorHandler: PlaybackErrorHandler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "SmartPlayerRouter"

        private val VLC_REQUIRED_TYPES = setOf(
            StreamType.TORRENT,
            StreamType.EXTERNAL
        )

        private val EXOPLAYER_EXTENSIONS = setOf(
            "mp4", "m4v", "mkv", "webm", "ts", "m3u8", "mpd"
        )

        private val VLC_PREFERRED_EXTENSIONS = setOf(
            "avi", "wmv", "flv", "rmvb", "rm", "3gp"
        )

        private val VLC_PREFERRED_CODECS = setOf(
            "hevc", "h265", "vp9", "av1"
        )
    }

    private val _routerState = MutableStateFlow(RouterState())
    val routerState: StateFlow<RouterState> = _routerState.asStateFlow()

    private var currentExoPlayer: ExoPlayer? = null
    private var currentPlayerType: PlayerType = PlayerType.NONE

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Route a stream to the appropriate player.
     *
     * @return [PlayerSelection.Ready] when a player is prepared, or
     *         [PlayerSelection.Failed] if neither player could be set up.
     */
    suspend fun routeStream(
        stream: ProviderStream,
        preferExoPlayer: Boolean = true
    ): PlayerSelection = withContext(ioDispatcher) {
        val playerType = determinePlayerType(stream, preferExoPlayer)
        Timber.tag(TAG).d("Routing stream to $playerType: ${stream.url}")

        _routerState.update {
            it.copy(currentStream = stream, selectedPlayer = playerType, isRouting = true)
        }

        val selection = when (playerType) {
            PlayerType.EXOPLAYER -> prepareExoPlayer(stream)
            PlayerType.VLC -> prepareVlcPlayer(stream)
            PlayerType.NONE -> PlayerSelection.Failed("No suitable player found")
        }

        _routerState.update {
            it.copy(
                isRouting = false,
                lastError = if (selection is PlayerSelection.Failed) selection.reason else null
            )
        }

        currentPlayerType = if (selection is PlayerSelection.Ready) playerType else PlayerType.NONE
        selection
    }

    /** Switch to the alternate player when the current one fails. */
    suspend fun switchToAlternatePlayer(): PlayerSelection? {
        val currentStream = _routerState.value.currentStream ?: return null
        val alternateType = when (currentPlayerType) {
            PlayerType.EXOPLAYER -> PlayerType.VLC
            PlayerType.VLC -> PlayerType.EXOPLAYER
            PlayerType.NONE -> return null
        }
        Timber.tag(TAG).d("Switching from $currentPlayerType to $alternateType")
        releaseCurrentPlayer()
        return when (alternateType) {
            PlayerType.EXOPLAYER -> prepareExoPlayer(currentStream)
            PlayerType.VLC -> prepareVlcPlayer(currentStream)
            PlayerType.NONE -> null
        }
    }

    /** Handle a playback error and determine / execute the recovery strategy. */
    suspend fun handlePlaybackError(
        error: Throwable,
        currentPosition: Long
    ): ErrorRecoveryResult {
        return when (val recovery = playbackErrorHandler.handleError(error)) {
            is PlaybackErrorHandler.RecoveryAction.Retry ->
                ErrorRecoveryResult.Retry(recovery.delayMs)

            is PlaybackErrorHandler.RecoveryAction.SwitchPlayer -> {
                val selection = switchToAlternatePlayer()
                if (selection is PlayerSelection.Ready) {
                    ErrorRecoveryResult.PlayerSwitched(selection, currentPosition)
                } else {
                    ErrorRecoveryResult.Fatal("Failed to switch player")
                }
            }

            is PlaybackErrorHandler.RecoveryAction.TryNextStream ->
                ErrorRecoveryResult.TryNextStream

            is PlaybackErrorHandler.RecoveryAction.Fatal ->
                ErrorRecoveryResult.Fatal(recovery.message)
        }
    }

    fun getCurrentPlayerType(): PlayerType = currentPlayerType

    fun getExoPlayer(): ExoPlayer? =
        if (currentPlayerType == PlayerType.EXOPLAYER) currentExoPlayer else null

    fun getVlcPlayer(): VlcPlayerAdapter? =
        if (currentPlayerType == PlayerType.VLC) vlcPlayerAdapter else null

    fun releaseCurrentPlayer() {
        when (currentPlayerType) {
            PlayerType.EXOPLAYER -> {
                currentExoPlayer?.release()
                currentExoPlayer = null
            }
            PlayerType.VLC -> vlcPlayerAdapter.stop()
            PlayerType.NONE -> Unit
        }
        currentPlayerType = PlayerType.NONE
    }

    fun releaseAll() {
        releaseCurrentPlayer()
        vlcPlayerAdapter.release()
        _routerState.update { RouterState() }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private fun determinePlayerType(stream: ProviderStream, preferExoPlayer: Boolean): PlayerType {
        if (stream.type in VLC_REQUIRED_TYPES) return PlayerType.VLC

        val extension = stream.url.substringAfterLast('.', "").substringBefore('?').lowercase()
        if (extension in VLC_PREFERRED_EXTENSIONS) return PlayerType.VLC

        stream.metadata?.codec?.let { codec ->
            if (codec.lowercase() in VLC_PREFERRED_CODECS && !hasHardwareSupport(codec)) {
                return PlayerType.VLC
            }
        }

        if (stream.type == StreamType.HLS || stream.type == StreamType.DASH) {
            return PlayerType.EXOPLAYER
        }

        return if (preferExoPlayer) PlayerType.EXOPLAYER else PlayerType.VLC
    }

    private fun prepareExoPlayer(stream: ProviderStream): PlayerSelection {
        return try {
            val player = currentExoPlayer ?: exoPlayerFactory.create().also { currentExoPlayer = it }
            PlayerSelection.Ready(
                playerType = PlayerType.EXOPLAYER,
                exoPlayer = player,
                vlcPlayer = null,
                streamUrl = stream.url,
                headers = stream.headers
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to prepare ExoPlayer")
            PlayerSelection.Failed("ExoPlayer initialization failed: ${e.message}")
        }
    }

    private suspend fun prepareVlcPlayer(stream: ProviderStream): PlayerSelection {
        return try {
            if (!_routerState.value.isVlcInitialized) {
                vlcPlayerAdapter.initialize().getOrThrow()
                _routerState.update { it.copy(isVlcInitialized = true) }
            }
            vlcPlayerAdapter.setMedia(stream.url, stream.headers)
            PlayerSelection.Ready(
                playerType = PlayerType.VLC,
                exoPlayer = null,
                vlcPlayer = vlcPlayerAdapter,
                streamUrl = stream.url,
                headers = stream.headers
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to prepare VLC")
            PlayerSelection.Failed("VLC initialization failed: ${e.message}")
        }
    }

    /**
     * Returns true when the device exposes a hardware decoder for [codec].
     * Uses [MediaCodecList] directly (no android.util.Log).
     */
    private fun hasHardwareSupport(codec: String): Boolean {
        val mimeType = when (codec.lowercase()) {
            "hevc", "h265" -> "video/hevc"
            "h264", "avc" -> "video/avc"
            "vp9" -> "video/x-vnd.on2.vp9"
            "av1" -> "video/av01"
            else -> return false
        }
        return runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                !info.isEncoder &&
                    info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } &&
                    info.isHardwareAccelerated
            }
        }.getOrDefault(false)
    }
}

// ─── Supporting types ─────────────────────────────────────────────────────────

enum class PlayerType { NONE, EXOPLAYER, VLC }

data class RouterState(
    val currentStream: ProviderStream? = null,
    val selectedPlayer: PlayerType = PlayerType.NONE,
    val isRouting: Boolean = false,
    val isVlcInitialized: Boolean = false,
    val lastError: String? = null
)

sealed class PlayerSelection {
    data class Ready(
        val playerType: PlayerType,
        val exoPlayer: ExoPlayer?,
        val vlcPlayer: VlcPlayerAdapter?,
        val streamUrl: String,
        val headers: Map<String, String>
    ) : PlayerSelection()

    data class Failed(val reason: String) : PlayerSelection()
}

sealed class ErrorRecoveryResult {
    data class Retry(val delayMs: Long) : ErrorRecoveryResult()
    data class PlayerSwitched(
        val newSelection: PlayerSelection.Ready,
        val resumePosition: Long
    ) : ErrorRecoveryResult()
    object TryNextStream : ErrorRecoveryResult()
    data class Fatal(val message: String) : ErrorRecoveryResult()
}
