package com.kurostream.players.backend

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.flow.StateFlow

sealed class PlayerBackendType {
    object Media3 : PlayerBackendType()
    object LibVLC : PlayerBackendType()
    object LibMPV : PlayerBackendType()
}

data class PlaybackCapabilities(
    val supportsHDR: Boolean = false,
    val supportsHDR10: Boolean = false,
    val supportsHDR10Plus: Boolean = false,
    val supportsDolbyVision: Boolean = false,
    val supportsHLG: Boolean = false,
    val supportsCodec: Set<String> = emptySet(),
    val supportsPassthrough: Boolean = false,
    val maxResolution: Pair<Int, Int> = 1920 to 1080,
    val supportedFrameRates: List<Float> = listOf(24f, 25f, 30f, 50f, 60f),
)

interface PlayerBackend {
    val type: PlayerBackendType
    val capabilities: PlaybackCapabilities
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val error: StateFlow<PlaybackException?>

    fun prepare(mediaItem: MediaItem)
    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setAudioTrack(trackId: String)
    fun setVideoTrack(trackId: String)
    fun setSubtitleTrack(trackId: String)
    fun release()

    companion object {
        fun selectBestBackend(
            available: List<PlayerBackend>,
            mediaItem: MediaItem,
            preferHDR: Boolean = true,
            preferPassthrough: Boolean = true,
        ): PlayerBackend {
            val codec = extractCodecFromMediaItem(mediaItem)
            val isHDR = isHDREncoded(mediaItem)

            return available
                .filter { it.isCodecSupported(codec) }
                .let { candidates ->
                    if (preferHDR && isHDR) {
                        candidates.filter { it.capabilities.supportsHDR }.maxByOrNull { it.capabilityScore() }
                    } else {
                        candidates.maxByOrNull { it.capabilityScore() }
                    }
                }
                ?: available.firstOrNull()
                ?: throw IllegalStateException("No available player backend")
        }

        private fun extractCodecFromMediaItem(mediaItem: MediaItem): String {
            return (mediaItem.localConfiguration?.tag as? String) ?: ""
        }

        private fun isHDREncoded(mediaItem: MediaItem): Boolean {
            val codec = extractCodecFromMediaItem(mediaItem)
            return codec.contains("hevc", ignoreCase = true) ||
                   codec.contains("h265", ignoreCase = true) ||
                   codec.contains("vp9", ignoreCase = true) ||
                   codec.contains("av1", ignoreCase = true)
        }

        private fun PlayerBackend.capabilityScore(): Int {
            var score = 0
            if (capabilities.supportsHDR) score += 10
            if (capabilities.supportsHDR10) score += 5
            if (capabilities.supportsDolbyVision) score += 8
            if (capabilities.supportsPassthrough) score += 3
            score += capabilities.supportedFrameRates.size
            return score
        }
    }
}