package com.kurostream.app.player

import androidx.compose.runtime.Immutable

@Immutable
data class PlayerUiState(
    val currentTitle: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1f,
    val subtitleFontSize: Float = 20f,
    val subtitleFontColorHex: String = "#FFFFFF",
    val subtitleBgColorHex: String = "#80000000",
    val subtitleEnabled: Boolean = true,
    // Skip-detection ranges (ms); 0 = not available
    val introStartMs: Long = 0L,
    val introEndMs: Long = 0L,
    val outroStartMs: Long = 0L,
    val outroEndMs: Long = 0L,
    // Episode navigation
    val nextEpisodeId: String? = null,
    val prevEpisodeId: String? = null,
)
