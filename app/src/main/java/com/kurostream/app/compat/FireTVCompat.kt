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
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import timber.log.Timber
import android.bluetooth.BluetoothDevice

/**
 * Fire TV compatibility layer for KuroStream.
 * 
 * Handles:
 * - Amazon Fire TV remote key events
 * - HDMI CEC commands
 * - Alexa voice assistant integration
 * - Low-memory optimizations for Fire TV Stick HD
 * - Network optimizations for streaming
 * - Audio focus handling for TV speakers
 */
object FireTVCompat {
    
    private const val TAG = "FireTVCompat"
    
    /**
     * Check if running on Amazon Fire TV device
     */
    fun isFireTV(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.equals("Amazon", ignoreCase = true)
        val model = Build.MODEL.contains("AFT", ignoreCase = true) ||
                    Build.MODEL.contains("Fire TV", ignoreCase = true) ||
                    Build.MODEL.contains("Kindle", ignoreCase = true)
        return manufacturer && model
    }
    
    /**
     * Check if running on Fire TV Stick HD (1st gen)
     */
    fun isFireTVStickHD(context: Context): Boolean {
        if (!isFireTV(context)) return false
        val totalMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        return totalMemMb <= 2048
    }
    
    /**
     * Check if running on Fire TV Stick 4K
     */
    fun isFireTVStick4K(context: Context): Boolean {
        if (!isFireTV(context)) return false
        val totalMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        return totalMemMb in 2049..4096
    }
    
    /**
     * Check if running on Fire TV Cube
     */
    fun isFireTVCube(context: Context): Boolean {
        if (!isFireTV(context)) return false
        val totalMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        return totalMemMb > 4096
    }
    
    /**
     * Get Fire TV device type string
     */
    fun getFireTVDeviceType(context: Context): String {
        return when {
            isFireTVCube(context) -> "firetv_cube"
            isFireTVStick4K(context) -> "firetv_stick_4k"
            isFireTVStickHD(context) -> "firetv_stick_hd"
            isFireTV(context) -> "firetv_unknown"
            else -> "not_firetv"
        }
    }
}

/**
 * Fire TV specific optimizations
 */
object FireTVOptimizations {
    
    data class OptimizationConfig(
        val maxImageCacheMb: Int = 40,
        val maxBitmapSize: Int = 1920 * 1080,
        val enableHardwareLayers: Boolean = true,
        val reduceAnimations: Boolean = true,
        val maxNetworkRequests: Int = 3,
        val bufferSizeKb: Int = 2048,
        val enableProgressiveLoading: Boolean = true,
        val disableBackgroundServices: Boolean = false,
    )
    
    fun getConfig(context: Context): OptimizationConfig {
        val totalMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        
        return when {
            totalMemMb <= 1536 -> OptimizationConfig(
                maxImageCacheMb = 30,
                maxBitmapSize = 1280 * 720,
                enableHardwareLayers = true,
                reduceAnimations = true,
                maxNetworkRequests = 2,
                bufferSizeKb = 1024,
                enableProgressiveLoading = true,
                disableBackgroundServices = true,
            )
            totalMemMb <= 2048 -> OptimizationConfig(
                maxImageCacheMb = 40,
                maxBitmapSize = 1920 * 1080,
                enableHardwareLayers = true,
                reduceAnimations = true,
                maxNetworkRequests = 3,
                bufferSizeKb = 2048,
                enableProgressiveLoading = true,
                disableBackgroundServices = false,
            )
            totalMemMb <= 4096 -> OptimizationConfig(
                maxImageCacheMb = 80,
                maxBitmapSize = 1920 * 1080,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 4,
                bufferSizeKb = 4096,
                enableProgressiveLoading = true,
                disableBackgroundServices = false,
            )
            else -> OptimizationConfig(
                maxImageCacheMb = 150,
                maxBitmapSize = 3840 * 2160,
                enableHardwareLayers = true,
                reduceAnimations = false,
                maxNetworkRequests = 6,
                bufferSizeKb = 8192,
                enableProgressiveLoading = false,
                disableBackgroundServices = false,
            )
        }
    }
}

/**
 * Fire TV remote key event handler
 */
object FireTVRemoteHandler {
    
    // Amazon Fire TV remote key codes
    const val KEY_MEDIA_PLAY_PAUSE = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
    const val KEY_MEDIA_STOP = KeyEvent.KEYCODE_MEDIA_STOP
    const val KEY_MEDIA_NEXT = KeyEvent.KEYCODE_MEDIA_NEXT
    const val KEY_MEDIA_PREVIOUS = KeyEvent.KEYCODE_MEDIA_PREVIOUS
    const val KEY_MEDIA_REWIND = KeyEvent.KEYCODE_MEDIA_REWIND
    const val KEY_MEDIA_FAST_FORWARD = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    
    // Alexa voice button
    const val KEY_VOICE_ASSIST = KeyEvent.KEYCODE_F11
    
    // Amazon specific buttons
    const val KEY_AMAZON_MENU = KeyEvent.KEYCODE_MENU
    const val KEY_AMAZON_BACK = KeyEvent.KEYCODE_BACK
    const val KEY_AMAZON_HOME = KeyEvent.KEYCODE_HOME
    
    /**
     * Check if the key event is from a Fire TV remote
     */
    fun isFireTVRemote(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        val name = device.name.lowercase()
        return name.contains("fire") || 
               name.contains("amazon") ||
               name.contains("aft") ||
               name.contains("voice") ||
               name.contains("alexa")
    }
    
    /**
     * Check if the key event is from a Bluetooth device (e.g., headphones)
     */
    fun isBluetoothDevice(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        // Simplified check - just verify it's a BT device by name
        val name = device.name?.lowercase() ?: ""
        return name.contains("bluetooth") || name.contains("airpod") || name.contains("headphone")
    }
    
    /**
     * Map Fire TV remote buttons to playback actions
     */
    fun mapKeyToPlaybackAction(keyCode: Int): PlaybackAction? {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> PlaybackAction.TOGGLE_PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY -> PlaybackAction.PLAY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> PlaybackAction.PAUSE
            KeyEvent.KEYCODE_MEDIA_STOP -> PlaybackAction.STOP
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> PlaybackAction.SEEK_FORWARD
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND -> PlaybackAction.SEEK_BACKWARD
            KeyEvent.KEYCODE_DPAD_UP -> PlaybackAction.SEEK_FORWARD_LONG
            KeyEvent.KEYCODE_DPAD_DOWN -> PlaybackAction.SEEK_BACKWARD_LONG
            KeyEvent.KEYCODE_DPAD_LEFT -> PlaybackAction.PREVIOUS_CHAPTER
            KeyEvent.KEYCODE_DPAD_RIGHT -> PlaybackAction.NEXT_CHAPTER
            else -> null
        }
    }
    
    enum class PlaybackAction {
        PLAY, PAUSE, STOP, TOGGLE_PLAY_PAUSE,
        SEEK_FORWARD, SEEK_BACKWARD,
        SEEK_FORWARD_LONG, SEEK_BACKWARD_LONG,
        NEXT_CHAPTER, PREVIOUS_CHAPTER
    }
}

/**
 * HDMI CEC handler for Fire TV
 */
object HDMICECHandler {
    
    private const val TAG = "HDMICECHandler"
    
    // CEC opcodes
    private const val CEC_OPCODE_STANDBY = 0x36
    private const val CEC_OPCODE_IMAGE_VIEW_ON = 0x04
    private const val CEC_OPCODE_TEXT_VIEW_ON = 0x0D
    private const val CEC_OPCODE_ACTIVE_SOURCE = 0x82
    private const val CEC_OPCODE_INACTIVE_SOURCE = 0x9D
    
    /**
     * Handle HDMI CEC command
     */
    fun handleCECCommand(command: ByteArray): CECAction? {
        if (command.size < 2) return false // FireTVCompat: default fallback
        
        val opcode = command[1]
        return when (opcode.toInt() and 0xFF) {
            CEC_OPCODE_STANDBY -> CECAction.STANDBY
            CEC_OPCODE_IMAGE_VIEW_ON, CEC_OPCODE_TEXT_VIEW_ON -> CECAction.WAKE_UP
            CEC_OPCODE_ACTIVE_SOURCE -> CECAction.ACTIVE_SOURCE
            CEC_OPCODE_INACTIVE_SOURCE -> CECAction.INACTIVE_SOURCE
            else -> null
        }
    }
    
    /**
     * Build CEC command for active source
     */
    fun buildActiveSourceCommand(physicalAddress: Int): ByteArray {
        return byteArrayOf(
            0x0F.toByte(), // destination: all
            CEC_OPCODE_ACTIVE_SOURCE.toByte(),
            (physicalAddress shr 8).toByte(),
            physicalAddress.toByte()
        )
    }
    
    enum class CECAction {
        STANDBY, WAKE_UP, ACTIVE_SOURCE, INACTIVE_SOURCE
    }
}

/**
 * Fire TV audio optimizer
 */
@Suppress("DEPRECATION")
object FireTVAudioOptimizer {
    
    /**
     * Configure audio for Fire TV speakers
     */
    fun optimizeForFireTVSpeaker(audioManager: AudioManager) {
        try {
            // Disable audio effects on low-end devices to save CPU
            if (Runtime.getRuntime().maxMemory() <= 2048 * 1024 * 1024) {
                audioManager.mode = AudioManager.MODE_NORMAL
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to optimize audio")
        }
    }
    
    /**
     * Handle audio focus for TV playback
     */
    fun handleAudioFocus(audioManager: AudioManager, gain: Boolean) {
        try {
            if (gain) {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
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
 * Fire TV network optimizer
 */
object FireTVNetworkOptimizer {
    
    /**
     * Check if network is suitable for streaming
     */
    fun isNetworkSuitableForStreaming(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
               (capabilities.linkDownstreamBandwidthKbps >= 5000)
    }
    
    /**
     * Get recommended buffer size based on network speed
     */
    fun getRecommendedBufferSize(context: Context): Int {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return 8192
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 8192
        
        val bandwidth = capabilities.linkDownstreamBandwidthKbps
        return when {
            bandwidth >= 20000 -> 16384 // 16MB for fast connections
            bandwidth >= 10000 -> 8192  // 8MB for moderate
            bandwidth >= 5000 -> 4096   // 4MB for slow
            else -> 2048                  // 2MB for very slow
        }
    }
}

/**
 * Fire TV window optimizations
 */
object FireTVWindowOptimizer {
    
    /**
     * Apply Fire TV specific window flags
     */
    fun applyFireTVFlags(activity: Activity) {
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
     * Remove Fire TV specific window flags
     */
    fun removeFireTVFlags(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

/**
 * Fire TV input device listener
 */
class FireTVInputDeviceListener(
    private val context: Context,
    private val onRemoteConnected: () -> Unit = {},
    private val onRemoteDisconnected: () -> Unit = {},
    private val onGamepadConnected: (Int) -> Unit = {},
    private val onGamepadDisconnected: (Int) -> Unit = {},
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    
    fun start() {
        isListening = true
        // Check already connected devices
        val devices = InputDevice.getDeviceIds()
        devices.forEach { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            if (device != null) {
                checkAndNotifyDevice(device)
            }
        }
    }
    
    fun stop() {
        isListening = false
    }
    
    private fun checkAndNotifyDevice(device: InputDevice) {
        val sources = device.sources
        val name = device.name.lowercase()
        
        when {
            sources and InputDevice.SOURCE_DPAD != 0 -> {
                if (name.contains("fire") || name.contains("amazon") || name.contains("alexa")) {
                    onRemoteConnected()
                }
            }
            sources and InputDevice.SOURCE_GAMEPAD != 0 -> {
                onGamepadConnected(device.id)
            }
        }
    }
}