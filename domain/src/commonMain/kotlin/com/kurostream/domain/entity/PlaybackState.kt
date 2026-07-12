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

package com.kurostream.domain.entity
import com.kurostream.domain.platform.platformCurrentTimeMillis

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val mediaId: String,
    val profileId: String,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val isFinished: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val audioTrackIndex: Int = 0,
    val subtitleTrackIndex: Int = -1,
    val lastPlayedAt: Long = platformCurrentTimeMillis()
) {
    val progressPercent: Float
        get() = if (durationMillis > 0) (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f) else 0f
    val isStarted: Boolean get() = positionMillis > 10_000L
}
