// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.os.Bundle
import android.os.Build
import android.os.PowerManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Rational
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import com.kurostream.app.ui.theme.AnimeStreamTVTheme
import com.kurostream.app.player.HdrMode
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private var wakeLock: PowerManager.WakeLock? = null
    private var hdmiReceiver: BroadcastReceiver? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val hdrDetector = HdrDetector

    companion object {
        private const val EXTRA_MEDIA_ID = "media_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_START_POSITION = "start_position_ms"

        fun createIntent(
            context: Context,
            mediaId: String,
            episodeId: String? = null,
            startPositionMs: Long = 0L
        ): Intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_EPISODE_ID, episodeId)
            putExtra(EXTRA_START_POSITION, startPositionMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE or android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID).orEmpty()
        if (mediaId.isBlank()) {
            finish()
            return
        }
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID).orEmpty()
        if (episodeId.isEmpty()) {
            Timber.e("PlayerActivity: No episode ID provided")
            finish()
            return
        }
        val startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val hdmiFilter = android.content.IntentFilter("android.intent.action.HDMI_PLUGGED")
                hdmiReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                        val plugged = intent?.getBooleanExtra("state", false) ?: true
                        if (!plugged) {
                            viewModel.setAudioPassthrough(false)
                        }
                    }
                }
                registerReceiver(hdmiReceiver, hdmiFilter)
            } catch (e: Exception) {
                Timber.w(e, "Failed to register HDMI audio receiver")
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        audioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        android.media.AudioManager.AUDIOFOCUS_LOSS -> viewModel.pause()
                        android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> viewModel.pause()
                        android.media.AudioManager.AUDIOFOCUS_GAIN -> viewModel.play()
                    }
                }
                .build()
        } else null

        viewModel.preparePlayback(mediaId, episodeId, startPosition)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                ?: throw IllegalStateException("DisplayManager not available")
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            display?.mode?.let { mode ->
                Timber.d("Display mode: ${mode.physicalWidth}x${mode.physicalHeight}@${mode.refreshRate}")
            }
        }

        hdrDetector.detect(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioFocusRequest?.let { af ->
                audioManager.requestAudioFocus(af)
            }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "KuroStream:PlayerWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)

        setContent {
            AnimeStreamTVTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    hdrMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        when {
                            hdrDetector.supportsDolbyVision(this@PlayerActivity) -> HdrMode.DOLBY_VISION
                            hdrDetector.supportsHdr10(this@PlayerActivity) -> HdrMode.HDR10
                            hdrDetector.supportsHdr10Plus(this@PlayerActivity) -> HdrMode.HDR10_PLUS
                            else -> HdrMode.SDR
                        }
                    } else HdrMode.SDR,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveProgress()
    }

    override fun onStop() {
        super.onStop()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hdmiReceiver?.let { unregisterReceiver(it) }
        hdmiReceiver = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { af ->
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.abandonAudioFocusRequest(af)
            }
        }
        viewModel.releasePlayer()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && viewModel.isPlaying.value) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
            )
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }
}
