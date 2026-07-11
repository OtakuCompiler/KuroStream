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

package com.kurostream.launcher.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes

enum class TileType {
    APP, SHORTCUT, SETTINGS, STREAMBOX
}

data class LauncherTile(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int = 0,
    val iconDrawable: Drawable? = null,
    val type: TileType,
    val packageName: String? = null,
    val activityName: String? = null,
    val intentAction: String? = null,
    val extras: Bundle? = null,
    val order: Int = 0
)
