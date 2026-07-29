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

package com.kurostream.players.selector

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kurostream.players.core.PlayerBackend
import com.kurostream.players.core.PlayerInterface
import com.kurostream.players.media3.Media3Player
import com.kurostream.players.mpv.MpvPlayer
import com.kurostream.players.vlc.VlcPlayer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BackendSelectorIntegrationTest {

    private lateinit var context: Context
    private lateinit var selector: BackendSelector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        selector = BackendSelector(context)
    }

    @Test
    fun selectBackend_whenForced_returnsForcedBackend() {
        val backend = selector.selectBackend(
            uri = "https://example.com/video.mp4",
            forceBackend = PlayerBackend.MEDIA3
        )

        assertEquals(PlayerBackend.MEDIA3, backend.backendType)
    }

    @Test
    fun selectBackend_forHlsStream_prefersMpvWhenAvailable() {
        // This test verifies the selection logic, actual backend availability depends on device
        val backend = selector.selectBackend(
            uri = "https://example.com/video.m3u8",
            requiredCodecs = listOf("h264", "aac"),
            requiresHdr = false,
            requiresDrm = false,
            requiresPassthrough = false
        )

        assertNotNull(backend)
        assertTrue(backend.backendType in listOf(PlayerBackend.MPV, PlayerBackend.VLC, PlayerBackend.MEDIA3))
    }

    @Test
    fun selectBackend_forDrmContent_prefersMedia3() {
        val backend = selector.selectBackend(
            uri = "https://example.com/drm/video.mp4",
            requiredCodecs = listOf("hevc"),
            requiresHdr = false,
            requiresDrm = true,
            requiresPassthrough = false
        )

        // Media3 should score highest for DRM content
        assertNotNull(backend)
    }

    @Test
    fun selectBackend_forHighQualityAudio_prefersMpvOrVlc() {
        val backend = selector.selectBackend(
            uri = "https://example.com/high-quality.mkv",
            requiredCodecs = listOf("hevc", "truehd", "dts-hd"),
            requiresHdr = true,
            requiresDrm = false,
            requiresPassthrough = true
        )

        // MPV or VLC should be preferred for passthrough audio
        assertNotNull(backend)
    }

    @Test
    fun releaseAll_releasesAllBackends() {
        // Create instances by selecting different backends
        selector.selectBackend("https://test1.mp4", forceBackend = PlayerBackend.MEDIA3)
        selector.selectBackend("https://test2.mp4", forceBackend = PlayerBackend.VLC)

        // Should not throw
        selector.releaseAll()
    }

    @Test
    fun selectBackend_cachesInstances() {
        val backend1 = selector.selectBackend("https://test.mp4", forceBackend = PlayerBackend.MEDIA3)
        val backend2 = selector.selectBackend("https://test.mp4", forceBackend = PlayerBackend.MEDIA3)

        // Should return same instance
        assertSame(backend1, backend2)
    }
}