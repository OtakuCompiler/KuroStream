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

    override suspend fun getString(key: Preferences.Key<String>, default: String): String {
        return dataStore.data.map { prefs ->
            prefs[key] ?: default
        }.first()
    }

    override suspend fun setString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    override suspend fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean): Boolean {
        return dataStore.data.map { prefs ->
            prefs[key] ?: default
        }.first()
    }

    override suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    override suspend fun getInt(key: Preferences.Key<Int>, default: Int): Int {
        return dataStore.data.map { prefs ->
            prefs[key] ?: default
        }.first()
    }

    override suspend fun setInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    override suspend fun getLong(key: Preferences.Key<Long>, default: Long): Long {
        return dataStore.data.map { prefs ->
            prefs[key] ?: default
        }.first()
    }

    override suspend fun setLong(key: Preferences.Key<Long>, value: Long) {
        dataStore.edit { it[key] = value }
    }

    override suspend fun getFloat(key: Preferences.Key<Float>, default: Float): Float {
        return dataStore.data.map { prefs ->
            prefs[key] ?: default
        }.first()
    }

    override suspend fun setFloat(key: Preferences.Key<Float>, value: Float) {
        dataStore.edit { it[key] = value }
    }

    override suspend fun editPreferences(block: suspend MutablePreferences.() -> Unit) {
        dataStore.edit { prefs ->
            prefs.block()
        }
    }
}
