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

package com.kurostream.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kurostream_settings")

@Singleton
class SettingsDataStoreImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : SettingsDataStore {

    private val dataStore: DataStore<Preferences> = context.dataStore

    override val data: Flow<Preferences> = dataStore.data

    override suspend fun getString(key: String, default: String): String {
        val prefs = dataStore.data.first()
        return prefs[stringPreferencesKey(key)] ?: default
    }

    override suspend fun setString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean {
        val prefs = dataStore.data.first()
        return prefs[booleanPreferencesKey(key)] ?: default
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    override suspend fun getInt(key: String, default: Int): Int {
        val prefs = dataStore.data.first()
        return prefs[intPreferencesKey(key)] ?: default
    }

    override suspend fun setInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    override suspend fun getLong(key: String, default: Long): Long {
        val prefs = dataStore.data.first()
        return prefs[longPreferencesKey(key)] ?: default
    }

    override suspend fun setLong(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    override suspend fun getFloat(key: String, default: Float): Float {
        val prefs = dataStore.data.first()
        return prefs[floatPreferencesKey(key)] ?: default
    }

    override suspend fun setFloat(key: String, value: Float) {
        dataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    override suspend fun editPreferences(block: suspend MutablePreferences.() -> Unit) {
        dataStore.edit { prefs ->
            prefs.block()
        }
    }
}