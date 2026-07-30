package com.kurostream.players.media3

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class Media3Player @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createExoPlayer(): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setMinVideoBitrate(300_000)
                    .setMaxVideoBitrate(80_000_000)
            )
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1500,
                8000,
                1000,
                2000
            )
            .setTargetBufferBytes(4 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setRenderersFactory(
                DefaultRenderersFactory(context).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                }
            )
            .setSeekParameters(Player.SeekParameters.CLOSEST_SYNC)
            .build()
    }
}
