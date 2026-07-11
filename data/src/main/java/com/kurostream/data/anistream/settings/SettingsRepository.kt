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

package com.kurostream.data.anistream.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao
) {

    private val dataStore = context.dataStore

    suspend fun getSetting(key: String): String? {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { it[prefKey] }.first()
    }

    suspend fun setSetting(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean {
        val prefKey = booleanPreferencesKey(key)
        return dataStore.data.map { it[prefKey] ?: default }.first()
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    suspend fun getInt(key: String, default: Int = 0): Int {
        val prefKey = intPreferencesKey(key)
        return dataStore.data.map { it[prefKey] ?: default }.first()
    }

    suspend fun setInt(key: String, value: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(key)] = value
        }
    }

    suspend fun getAllSettings(): Map<String, String> {
        return dataStore.data.first().asMap()
            .filter { it.key is Preferences.Key<*> }
            .mapKeys { it.key.name }
            .mapValues { it.value.toString() }
    }

    suspend fun clearCache() {
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }

    // Legacy Room backup support
    suspend fun getAllRoomSettings(): List<SettingEntity> = settingsDao.getAll()
}
