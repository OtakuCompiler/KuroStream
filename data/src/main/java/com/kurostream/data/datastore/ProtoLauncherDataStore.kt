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

package com.kurostream.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.kurostream.datastore.LauncherProto
import com.kurostream.launcher.ui.LauncherTile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtoLauncherDataStore @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<LauncherProto.LauncherPreferences> = context.dataStore(
        fileName = "launcher_prefs.pb",
        serializer = LauncherSerializer
    )

    val launcherPrefsFlow: Flow<LauncherProto.LauncherPreferences> = dataStore.data

    fun getCustomTiles(): Flow<List<String>> = dataStore.data.map { it.customTilesList }

    suspend fun saveCustomTiles(tiles: List<LauncherTile>) {
        dataStore.updateData { currentPrefs ->
            currentPrefs.toBuilder().apply {
                clearCustomTiles()
                addAllCustomTiles(tiles.map { it.toString() })
            }.build()
        }
    }

    fun isLauncherEnabled(): Flow<Boolean> = dataStore.data.map { it.launcherEnabled }

    suspend fun setLauncherEnabled(enabled: Boolean) {
        dataStore.updateData { currentPrefs ->
            currentPrefs.toBuilder().setLauncherEnabled(enabled).build()
        }
    }
}

val Context.launcherDataStore: DataStore<LauncherProto.LauncherPreferences>
    by dataStore(
        fileName = "launcher_prefs.pb",
        serializer = LauncherSerializer
    )
