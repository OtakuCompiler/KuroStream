/*
 * Desktop playback — uses libVLC (libvlc) for hardware-accelerated decoding.
 * VLC supports Direct3D11, VAAPI, VideoToolbox, NVDEC and Dolby Atmos
 * passthrough out of the box.
 *
 * The :playback module on Android already uses Media3/ExoPlayer, but libVLC
 * has a wider codec/HW-decoder support matrix on Linux & Windows. So on
 * desktop we bypass Media3 and talk to VLC directly.
 *
 * For webOS/Tizen (HTML5 host), we ship a WebAssembly port of FFmpeg that
 * exposes the same surface API but lives in the browser process — see
 * `tizen/src/main/wasm/`.
 */
package com.kurostream.desktop.playback

import androidx.compose.runtime.Stable
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

@Stable
class DesktopPlayerFactory {

    /**
     * Lazy-loaded libVLC discovery — works on Linux (LD_LIBRARY_PATH / libvlc.so.*),
     * Windows (PATH / libvlc.dll), macOS (DYLD_LIBRARY_PATH / libvlc.dylib).
     * On first playback attempt, vlcj searches the system for the native binaries.
     */
    private val discovery = NativeDiscovery()
    private val active = ConcurrentHashMap<String, PlayerHandle>()

    fun acquire(mediaId: String): PlayerHandle {
        return active.computeIfAbsent(mediaId) { id ->
            PlayerHandle(
                id = id,
                embeddedComponent = EmbeddedMediaPlayerComponent(),
                audioComponent = AudioPlayerComponent(),
            )
        }
    }

    fun releaseAll() {
        active.values.forEach { it.close() }
        active.clear()
    }

    fun playbackBackendAvailable(): Boolean = discovery.discover() != null
}

class PlayerHandle internal constructor(
    val id: String,
    val embeddedComponent: EmbeddedMediaPlayerComponent,
    val audioComponent: AudioPlayerComponent,
) : Closeable {
    val videoSurface get() = embeddedComponent.videoSurfaceComponent()
    val player: EmbeddedMediaPlayer get() = embeddedComponent.mediaPlayer()

    fun prepare(url: String) {
        player.media().play(url)
    }

    fun play() { player.controls().play() }
    fun pause() { player.controls().pause() }
    fun stop() { player.controls().stop() }
    fun seekTo(ms: Long) { player.controls().setTime(ms) }
    fun setVolume(v: Float) { player.audio().setVolume(v.coerceIn(0f, 100f).toInt()) }
    fun setAudioPassthrough(enabled: Boolean) {
        player.audio().isAudioPassthroughEnabled = enabled
    }

    override fun close() {
        runCatching { player.controls().stop() }
        runCatching { embeddedComponent.release() }
        runCatching { audioComponent.release() }
    }
}
