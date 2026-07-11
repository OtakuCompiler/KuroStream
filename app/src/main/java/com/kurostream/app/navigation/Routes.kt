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

package com.kurostream.app.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DetailsRoute(val mediaId: String)

@Serializable
object SearchRoute

@Serializable
object DownloadsRoute

@Serializable
object SettingsRoute

@Serializable
object AddonsRoute

@Serializable
data class PlayerRoute(
    val mediaId: String,
    val episodeId: String? = null,
    val startPositionMs: Long = 0L
)

@Serializable
object TorrentsRoute

@Serializable
object BackupRoute

@Serializable
object SourceLockSettingsRoute