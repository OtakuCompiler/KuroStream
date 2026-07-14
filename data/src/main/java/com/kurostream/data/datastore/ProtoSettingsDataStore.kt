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
import com.kurostream.datastore.SettingsProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtoSettingsDataStore @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<SettingsProto.Settings> = context.dataStore(
        fileName = "kurostream_settings.pb",
        serializer = SettingsSerializer
    )

    val settingsFlow: Flow<SettingsProto.Settings> = dataStore.data

    suspend fun updateSettings(block: SettingsProto.Settings.Builder.() -> Unit) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder().apply(block).build()
        }
    }

    fun getSettingsFlow(): Flow<SettingsProto.Settings> = dataStore.data
}

val Context.settingsDataStore: DataStore<SettingsProto.Settings>
    by dataStore(
        fileName = "kurostream_settings.pb",
        serializer = SettingsSerializer
    )
