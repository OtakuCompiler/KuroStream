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

package com.kurostream.benchmark

import android.os.SystemClock
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kurostream.players.core.PlayerBackend
import com.kurostream.players.core.PlayerInterface
import com.kurostream.players.media3.Media3Player
import com.kurostream.players.mpv.MpvPlayer
import com.kurostream.players.vlc.VlcPlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmark_media3Player_load() {
        val player: PlayerInterface = Media3Player(androidx.test.core.app.ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            player.loadMedia("https://test.example.com/video.mp4")
            player.play()
            Thread.sleep(100)
            player.stop()
        }
        player.release()
    }

    @Test
    fun benchmark_mpvPlayer_load() {
        val player: PlayerInterface = MpvPlayer(androidx.test.core.app.ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            player.loadMedia("https://test.example.com/video.mp4")
            player.play()
            Thread.sleep(100)
            player.stop()
        }
        player.release()
    }

    @Test
    fun benchmark_vlcPlayer_load() {
        val player: PlayerInterface = VlcPlayer(androidx.test.core.app.ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            player.loadMedia("https://test.example.com/video.mp4")
            player.play()
            Thread.sleep(100)
            player.stop()
        }
        player.release()
    }

    @Test
    fun benchmark_backendSelection() {
        val selector = com.kurostream.players.selector.BackendSelector(androidx.test.core.app.ApplicationProvider.getApplicationContext())

        benchmarkRule.measureRepeated {
            val backend = selector.selectBackend(
                uri = "https://test.example.com/video.mp4",
                requiredCodecs = listOf("hevc", "av1"),
                requiresHdr = true,
                requiresDrm = false,
                requiresPassthrough = true
            )
            backend.release()
        }
    }
}