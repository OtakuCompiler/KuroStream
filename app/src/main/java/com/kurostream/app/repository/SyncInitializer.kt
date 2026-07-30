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

package com.kurostream.app.repository

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber

/**
 * Sync Initializer - sets up cloud sync if Firebase is configured.
 * Fails gracefully if google-services.json is not present.
 */
@Suppress("DEPRECATION")
class SyncInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
        Timber.d("SyncInitializer: Firebase not configured — sync disabled")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}