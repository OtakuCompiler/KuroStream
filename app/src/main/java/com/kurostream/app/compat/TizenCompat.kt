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
 * Samsung Tizen compatibility layer for KuroStream.
 * 
 * Handles:
 * - Tizen specific input events (Smart Remote)
 * - Audio focus handling for TV speakers
 * - Memory constraints for Tizen TVs
 * - HbbTV support for hybrid broadcast
 * - WebKit-based playback
 */
object TizenCompat {
    
    private const val TAG = "TizenCompat"
    
    // Tizen device identifiers
    private val TIZEN_MANUFACTURERS = listOf("Samsung", "samsung")
    private val TIZEN_MODEL_PATTERNS = listOf(
        "tizen", "tu", "uq", "qn", "qn85", "qn90", "qn95", "ls", "au", "bu"
    )
    
    /**
     * Check if running on Samsung Tizen TV
     */
    fun isTizen(context: Context): Boolean {
        val manufacturer = TIZEN_MANUFACTURERS.any { 
            Build.MANUFACTURER.equals(it, ignoreCase = true) 
        }
        val model = TIZEN_MODEL_PATTERNS.any { 
            Build.MODEL.lowercase().contains(it) 
        }
        return manufacturer && model
    }
    
    /**
     * Get Tizen TV type
     */
    fun getTizenType(context: Context): TizenType {
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("qn90") || model.contains("qn95") -> TizenType.NEO_QLED
            model.contains("qn85") -> TizenType.QLED
            model.contains("tu") -> TizenType.CRYSTAL_UHD
            model.contains("au") -> TizenType.UHD
            model.contains("bu") -> TizenType.FHD
            model.contains("ls") -> TizenType.Lifestyle
            else -> TizenType.STANDARD
        }
    }
    
    /**
     * Check if device supports 8K resolution
     */
    fun supports8K(context: Context): Boolean {
        val type = getTizenType(context)
        return type == TizenType.NEO_QLED
    }
    
    /**
     * Check if device supports 4K resolution
     */
    fun supports4K(context: Context): Boolean {
        val type = getTizenType(context)
        return type in listOf(TizenType.NEO_QLED, TizenType.QLED, TizenType.CRYSTAL_UHD, TizenType.UHD)
    }
    
    /**
     * Check if device supports HDR10+
     */
    fun supportsHDR10Plus(context: Context): Boolean {
        val type = getTizenType(context)
        return type in listOf(TizenType.NEO_QLED, TizenType.QLED, TizenType.CRYSTAL_UHD)
    }
    
    /**
     * Check if device supports Dolby Vision
     */
    fun supportsDolbyVision(context: Context): Boolean {
        val type = getTizenType(context)
        // Note: 2022+ Samsung TVs support DV
        return type in listOf(TizenType.NEO_QLED, TizenType.QLED)
    }
    
    enum class TizenType {
        NEO_QLED,    // 8K QLED with MiniLED
        QLED,        // 4K QLED
        CRYSTAL_UHD, // Crystal UHD (basic 4K)
        UHD,         // Standard 4K UHD
        FHD,         // Full HD
        Lifestyle,   // The Frame, Serif, etc.
        STANDARD     // 1080p or lower
    }
}

/**
 * Tizen specific optimizations
 */
object TizenOptimizations {
    
    data class OptimizationConfig(
        val maxImageCacheMb: Int = 100,
        val maxBitmapSize: Int = 3840 * 2160,
        val enableHardwareLayers: Boolean = true,
        val reduceAnimations: Boolean = false,
        val maxNetworkRequests: Int = 6,
        val bufferSizeKb: Int = 8192,
        val enableProgressiveLoading: Boolean = true,
        val enableHbbTV: Boolean = true,
        val enableWebViewFallback: Boolean = true,
        val enableAVSync: Boolean = true,
    )
    
    fun getConfig(context: Context): OptimizationConfig {
        val type = TizenCompat.getTizenType(context)
        
        return when (type) {
            TizenCompat.TizenType.NEO_QLED -> OptimizationConfig(
                maxImageCacheMb = 200,
                maxBitmapSize = 7680 * 4320, // 8K
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 8,
                bufferSizeKb = 16384,
                enableProgressiveLoading = false,
                enableHbbTV = true,
                enableWebViewFallback = false,
                enableAVSync = true,
            )
            TizenCompat.TizenType.QLED -> OptimizationConfig(
                maxImageCacheMb = 150,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 6,
                bufferSizeKb = 8192,
                enableProgressiveLoading = false,
                enableHbbTV = true,
                enableWebViewFallback = false,
                enableAVSync = true,
            )
            TizenCompat.TizenType.CRYSTAL_UHD -> OptimizationConfig(
                maxImageCacheMb = 100,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 4,
                bufferSizeKb = 8192,
                enableProgressiveLoading = true,
                enableHbbTV = true,
                enableWebViewFallback = true,
                enableAVSync = true,
            )
            TizenCompat.TizenType.UHD -> OptimizationConfig(
                maxImageCacheMb = 80,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 4,
                bufferSizeKb = 4096,
                enableProgressiveLoading = true,
                enableHbbTV = true,
                enableWebViewFallback = true,
                enableAVSync = true,
            )
            TizenCompat.TizenType.FHD -> OptimizationConfig(
                maxImageCacheMb = 60,
                maxBitmapSize = 1920 * 1080,
                enableHardwareLayers = true,
                reduceAnimations = true,
                maxNetworkRequests = 3,
                bufferSizeKb = 4096,
                enableProgressiveLoading = true,
                enableHbbTV = false,
                enableWebViewFallback = true,
                enableAVSync = true,
            )
            TizenCompat.TizenType.Lifestyle -> OptimizationConfig(
                maxImageCacheMb = 80,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 4,
                bufferSizeKb = 4096,
                enableProgressiveLoading = true,
                enableHbbTV = true,
                enableWebViewFallback = true,
                enableAVSync = true,
            )
            TizenCompat.TizenType.STANDARD -> OptimizationConfig(
                maxImageCacheMb = 50,
                maxBitmapSize = 1920 * 1080,
                enableHardwareLayers = true,
                reduceAnimations = true,
                maxNetworkRequests = 3,
                bufferSizeKb = 2048,
                enableProgressiveLoading = true,
                enableHbbTV = false,
                enableWebViewFallback = true,
                enableAVSync = false,
            )
        }
    }
}

/**
 * Tizen Smart Remote input handler
 */
object TizenInputHandler {
    
    // Tizen Smart Remote button codes
    const val KEY_UP = 38
    const val KEY_DOWN = 40
    const val KEY_LEFT = 37
    const val KEY_RIGHT = 39
    const val KEY_ENTER = 13
    const val KEY_BACK = 10009
    const val KEY_HOME = 10252
    const val KEY_MENU = 229
    const val KEY_PLAY = 165
    const val KEY_PAUSE = 163
    const val KEY_STOP = 128
    const val KEY_REWIND = 168
    const val KEY_FORWARD = 208
    const val KEY_TRACK_NEXT = 208
    const val KEY_TRACK_PREV = 167
    const val KEY_RECORD = 167
    const val KEY_RED = 403
    const val KEY_GREEN = 404
    const val KEY_YELLOW = 405
    const val KEY_BLUE = 406
    const val KEY_VOL_UP = 115
    const val KEY_VOL_DOWN = 114
    const val KEY_MUTE = 113
    const val KEY_CHANNEL_UP = 427
    const val KEY_CHANNEL_DOWN = 428
    const val KEY_GUIDE = 502
    const val KEY_INFO = 457
    const val KEY_TOOLS = 10200
    const val KEY_CONTENT = 601
    const val KEY_TELETEXT = 10190
    const val KEY_SUBTITLE = 10232
    const val KEY_3D = 10191
    const val KEY_PICTURE_SIZE = 605
    const val KEY_AD = 70
    const val KEY_SLEEP = 10154
    const val KEY_DTV = 10190
    
    /**
     * Map Tizen remote button to playback action
     */
    fun mapButtonToPlaybackAction(keyCode: Int): TizenPlaybackAction? {
        return when (keyCode) {
            KEY_PLAY -> TizenPlaybackAction.PLAY
            KEY_PAUSE -> TizenPlaybackAction.PAUSE
            KEY_STOP -> TizenPlaybackAction.STOP
            KEY_REWIND -> TizenPlaybackAction.SEEK_BACKWARD
            KEY_FORWARD -> TizenPlaybackAction.SEEK_FORWARD
            KEY_TRACK_NEXT -> TizenPlaybackAction.NEXT_TRACK
            KEY_TRACK_PREV -> TizenPlaybackAction.PREV_TRACK
            KEY_ENTER -> TizenPlaybackAction.TOGGLE_PLAY_PAUSE
            else -> null
        }
    }
    
    /**
     * Map Tizen remote button to navigation action
     */
    fun mapButtonToNavigationAction(keyCode: Int): TizenNavigationAction? {
        return when (keyCode) {
            KEY_UP -> TizenNavigationAction.UP
            KEY_DOWN -> TizenNavigationAction.DOWN
            KEY_LEFT -> TizenNavigationAction.LEFT
            KEY_RIGHT -> TizenNavigationAction.RIGHT
            KEY_ENTER -> TizenNavigationAction.SELECT
            KEY_BACK -> TizenNavigationAction.BACK
            KEY_HOME -> TizenNavigationAction.HOME
            KEY_MENU -> TizenNavigationAction.MENU
            KEY_GUIDE -> TizenNavigationAction.GUIDE
            KEY_INFO -> TizenNavigationAction.INFO
            KEY_TOOLS -> TizenNavigationAction.TOOLS
            KEY_CONTENT -> TizenNavigationAction.SOURCE
            else -> null
        }
    }
    
    enum class TizenPlaybackAction {
        PLAY, PAUSE, STOP, TOGGLE_PLAY_PAUSE,
        SEEK_FORWARD, SEEK_BACKWARD,
        NEXT_TRACK, PREV_TRACK
    }
    
    enum class TizenNavigationAction {
        UP, DOWN, LEFT, RIGHT, SELECT, BACK, HOME, MENU, GUIDE, INFO, TOOLS, SOURCE
    }
}

/**
 * Tizen HbbTV support
 */
object TizenHbbTVManager {
    
    private const val TAG = "TizenHbbTVManager"
    
    /**
     * Check if HbbTV is supported
     */
    fun isHbbTVSupported(context: Context): Boolean {
        return TizenCompat.isTizen(context)
    }
    
    /**
     * Get HbbTV app URL for a channel
     */
    fun getHbbTVAppUrl(broadcastUrl: String, appName: String): String? {
        // HbbTV apps are typically at a specific URL pattern
        // This is a simplified implementation
        return try {
            val baseUrl = broadcastUrl.substringBefore("/", broadcastUrl)
            "$baseUrl/hbbtv/$appName"
        } catch (e: Exception) {
            Timber.e(e, "Failed to construct HbbTV URL")
            null
        }
    }
    
    /**
     * Launch HbbTV application
     */
    fun launchHbbTVApp(context: Context, appUrl: String): Boolean {
        if (!isHbbTVSupported(context)) {
            Timber.w(TAG, "HbbTV not supported")
            return false
        }
        
        // In a real implementation, this would launch the HbbTV app
        // via the Tizen platform API
        Timber.d(TAG, "Launching HbbTV app: $appUrl")
        return true
    }
}

/**
 * Tizen audio session manager
 */
@Suppress("DEPRECATION")
object TizenAudioManager {
    
    /**
     * Configure audio for Tizen TV speakers
     */
    fun configureAudio(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            
            // Tizen TVs have good audio processing
            audioManager.mode = android.media.AudioManager.MODE_NORMAL
            
            // Enable TV speaker
            audioManager.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Timber.w(e, "Failed to configure Tizen audio")
        }
    }
    
    /**
     * Handle audio focus for Tizen
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
 * Tizen window optimizations
 */
object TizenWindowOptimizer {
    
    /**
     * Apply Tizen specific window flags
     */
    fun applyTizenFlags(activity: Activity) {
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
     * Remove Tizen specific window flags
     */
    fun removeTizenFlags(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

/**
 * Unified platform compatibility interface
 */
object PlatformCompat {
    
    /**
     * Get the current platform type
     */
    fun getPlatformType(context: Context): PlatformType {
        return when {
            FireTVCompat.isFireTV(context) -> PlatformType.FIRE_TV
            WebOSCompat.isWebOS(context) -> PlatformType.WEB_OS
            TizenCompat.isTizen(context) -> PlatformType.TIZEN
            else -> PlatformType.ANDROID
        }
    }
    
    /**
     * Check if running on a TV platform
     */
    fun isTVPlatform(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature("android.software.leanback")
    }
    
    /**
     * Get platform-specific optimizations
     */
    fun getOptimizationConfig(context: Context): Any {
        return when (getPlatformType(context)) {
            PlatformType.FIRE_TV -> FireTVOptimizations.getConfig(context)
            PlatformType.WEB_OS -> WebOSOptimizations.getConfig(context)
            PlatformType.TIZEN -> TizenOptimizations.getConfig(context)
            PlatformType.ANDROID -> FireTVOptimizations.getConfig(context) // Default to Fire TV config
        }
    }
    
    enum class PlatformType {
        ANDROID,
        FIRE_TV,
        WEB_OS,
        TIZEN
    }
}