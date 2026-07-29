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

package com.kurostream.app.compat

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import timber.log.Timber

/**
 * LG webOS compatibility layer for KuroStream.
 * 
 * Handles:
 * - webOS specific input events (Magic Remote)
 * - Picture-in-Picture mode
 * - Audio focus handling for TV speakers
 * - Memory constraints for webOS TVs
 * - WebKit-based WebView for HTML5 playback
 */
object WebOSCompat {
    
    private const val TAG = "WebOSCompat"
    
    // webOS device identifiers
    private val WEBOS_MANUFACTURERS = listOf("LG", "lge")
    private val WEBOS_MODEL_PATTERNS = listOf(
        "webos", "web_os", "lgtv", "oled", "nano", "qned"
    )
    
    /**
     * Check if running on LG webOS TV
     */
    fun isWebOS(context: Context): Boolean {
        val manufacturer = WEBOS_MANUFACTURERS.any { 
            Build.MANUFACTURER.equals(it, ignoreCase = true) 
        }
        val model = WEBOS_MODEL_PATTERNS.any { 
            Build.MODEL.lowercase().contains(it) 
        }
        return manufacturer && model
    }
    
    /**
     * Get webOS TV type
     */
    fun getWebOSType(context: Context): WebOSType {
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("oled") -> WebOSType.OLED
            model.contains("nano") -> WebOSType.NANO_CELL
            model.contains("qned") -> WebOSType.QNED
            model.contains("uhd") -> WebOSType.UHD
            else -> WebOSType.STANDARD
        }
    }
    
    /**
     * Check if device supports 4K resolution
     */
    fun supports4K(context: Context): Boolean {
        val type = getWebOSType(context)
        return type in listOf(WebOSType.OLED, WebOSType.NANO_CELL, WebOSType.QNED)
    }
    
    /**
     * Check if device supports HDR
     */
    fun supportsHDR(context: Context): Boolean {
        val type = getWebOSType(context)
        return type in listOf(WebOSType.OLED, WebOSType.NANO_CELL, WebOSType.QNED)
    }
    
    enum class WebOSType {
        OLED,        // Premium OLED TVs
        NANO_CELL,   // Mid-range with NanoCell
        QNED,        // High-end with Quantum NanoCell
        UHD,         // Standard 4K UHD
        STANDARD     // 1080p or lower
    }
}

/**
 * webOS specific optimizations
 */
object WebOSOptimizations {
    
    data class OptimizationConfig(
        val maxImageCacheMb: Int = 80,
        val maxBitmapSize: Int = 3840 * 2160,
        val enableHardwareLayers: Boolean = true,
        val reduceAnimations: Boolean = false,
        val maxNetworkRequests: Int = 6,
        val bufferSizeKb: Int = 8192,
        val enableProgressiveLoading: Boolean = true,
        val enablePipMode: Boolean = true,
        val enableWebViewFallback: Boolean = true,
    )
    
    fun getConfig(context: Context): OptimizationConfig {
        val type = WebOSCompat.getWebOSType(context)
        
        return when (type) {
            WebOSCompat.WebOSType.OLED, WebOSCompat.WebOSType.QNED -> OptimizationConfig(
                maxImageCacheMb = 150,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 8,
                bufferSizeKb = 16384,
                enableProgressiveLoading = false,
                enablePipMode = true,
                enableWebViewFallback = false,
            )
            WebOSCompat.WebOSType.NANO_CELL -> OptimizationConfig(
                maxImageCacheMb = 100,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 6,
                bufferSizeKb = 8192,
                enableProgressiveLoading = true,
                enablePipMode = true,
                enableWebViewFallback = false,
            )
            WebOSCompat.WebOSType.UHD -> OptimizationConfig(
                maxImageCacheMb = 80,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 4,
                bufferSizeKb = 8192,
                enableProgressiveLoading = true,
                enablePipMode = true,
                enableWebViewFallback = true,
            )
            WebOSCompat.WebOSType.STANDARD -> OptimizationConfig(
                maxImageCacheMb = 60,
                maxBitmapSize = 1920 * 1080,
                enableHardwareLayers = true,
                reduceAnimations = true,
                maxNetworkRequests = 3,
                bufferSizeKb = 4096,
                enableProgressiveLoading = true,
                enablePipMode = false,
                enableWebViewFallback = true,
            )
        }
    }
}

/**
 * webOS Magic Remote input handler
 */
object WebOSInputHandler {
    
    // Magic Remote button codes
    const val KEY_BACK = 27
    const val KEY_HOME = 36
    const val KEY_MENU = 229
    const val KEY_UP = 19
    const val KEY_DOWN = 20
    const val KEY_LEFT = 21
    const val KEY_RIGHT = 22
    const val KEY_ENTER = 23
    const val KEY_VOL_UP = 448
    const val KEY_VOL_DOWN = 447
    const val KEY_MUTE = 448
    const val KEY_CHANNEL_UP = 427
    const val KEY_CHANNEL_DOWN = 428
    const val KEY_PLAY = 415
    const val KEY_PAUSE = 19
    const val KEY_STOP = 413
    const val KEY_REWIND = 412
    const val KEY_FORWARD = 417
    const val KEY_SKIP_FORWARD = 470
    const val KEY_SKIP_BACKWARD = 471
    const val KEY_RECORD = 416
    const val KEY_LIVE_TV = 1025
    const val KEY_3D = 10189
    const val KEY_SIMPLE_MENU = 1026
    const val KEY_TELETEXT = 10190
    
    /**
     * Map webOS remote button to playback action
     */
    fun mapButtonToPlaybackAction(keyCode: Int): WebOSPlaybackAction? {
        return when (keyCode) {
            KEY_PLAY -> WebOSPlaybackAction.PLAY
            KEY_PAUSE -> WebOSPlaybackAction.PAUSE
            KEY_STOP -> WebOSPlaybackAction.STOP
            KEY_REWIND -> WebOSPlaybackAction.SEEK_BACKWARD
            KEY_FORWARD -> WebOSPlaybackAction.SEEK_FORWARD
            KEY_SKIP_FORWARD -> WebOSPlaybackAction.NEXT_TRACK
            KEY_SKIP_BACKWARD -> WebOSPlaybackAction.PREV_TRACK
            KEY_ENTER -> WebOSPlaybackAction.TOGGLE_PLAY_PAUSE
            else -> null
        }
    }
    
    /**
     * Map webOS remote button to navigation action
     */
    fun mapButtonToNavigationAction(keyCode: Int): WebOSNavigationAction? {
        return when (keyCode) {
            KEY_UP -> WebOSNavigationAction.UP
            KEY_DOWN -> WebOSNavigationAction.DOWN
            KEY_LEFT -> WebOSNavigationAction.LEFT
            KEY_RIGHT -> WebOSNavigationAction.RIGHT
            KEY_ENTER -> WebOSNavigationAction.SELECT
            KEY_BACK -> WebOSNavigationAction.BACK
            KEY_HOME -> WebOSNavigationAction.HOME
            KEY_MENU -> WebOSNavigationAction.MENU
            else -> null
        }
    }
    
    enum class WebOSPlaybackAction {
        PLAY, PAUSE, STOP, TOGGLE_PLAY_PAUSE,
        SEEK_FORWARD, SEEK_BACKWARD,
        NEXT_TRACK, PREV_TRACK
    }
    
    enum class WebOSNavigationAction {
        UP, DOWN, LEFT, RIGHT, SELECT, BACK, HOME, MENU
    }
}

/**
 * webOS Picture-in-Picture manager
 */
object WebOSPipManager {
    
    private const val TAG = "WebOSPipManager"
    
    /**
     * Check if PiP mode is supported
     */
    fun isPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && WebOSCompat.isWebOS(context)
    }
    
    /**
     * Enter PiP mode
     */
    fun enterPip(activity: Activity): Boolean {
        if (!isPipSupported(activity)) {
            Timber.w(TAG, "PiP not supported on this device")
            return false
        }
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to enter PiP mode")
            false
        }
    }
    
    /**
     * Update PiP params (e.g., video aspect ratio)
     */
    fun updatePipParams(activity: Activity, aspectRatio: Float) {
        if (!isPipSupported(activity)) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val rational = if (aspectRatio > 1.5f) {
                    android.util.Rational(16, 9)
                } else {
                    android.util.Rational(4, 3)
                }
                
                activity.enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(rational)
                        .build()
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update PiP params")
        }
    }
}

/**
 * webOS audio session manager
 */
@Suppress("DEPRECATION")
object WebOSAudioManager {
    
    /**
     * Configure audio for webOS TV speakers
     */
    fun configureAudio(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            
            // webOS TVs typically have good speakers, enable all audio processing
            audioManager.mode = android.media.AudioManager.MODE_NORMAL
            
            // Set to TV speaker output
            audioManager.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Timber.w(e, "Failed to configure webOS audio")
        }
    }
    
    /**
     * Handle audio focus for webOS
     */
    fun handleAudioFocus(context: Context, gain: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            
            if (gain) {
                audioManager.requestAudioFocus(
                    null,
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.AUDIOFOCUS_GAIN
                )
            } else {
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to handle audio focus")
        }
    }
}

/**
 * webOS window optimizations
 */
object WebOSWindowOptimizer {
    
    /**
     * Apply webOS specific window flags
     */
    fun applyWebOSFlags(activity: Activity) {
        activity.window.apply {
            // Keep screen on during playback
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Prevent screenshots for DRM content
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    
    /**
     * Remove webOS specific window flags
     */
    fun removeWebOSFlags(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}