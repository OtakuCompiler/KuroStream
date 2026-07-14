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

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

interface SettingsDataStore {
    suspend fun getString(key: String, default: String): String
    suspend fun setString(key: String, value: String)

    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun setBoolean(key: String, value: Boolean)

    suspend fun getInt(key: String, default: Int): Int
    suspend fun setInt(key: String, value: Int)

    suspend fun getLong(key: String, default: Long): Long
    suspend fun setLong(key: String, value: Long)

    suspend fun getFloat(key: String, default: Float): Float
    suspend fun setFloat(key: String, value: Float)

    val data: Flow<Preferences>

    suspend fun editPreferences(block: suspend MutablePreferences.() -> Unit)

    object Keys {
        val SOURCE_LOCK_ENABLED = booleanPreferencesKey("source_lock_enabled")
        val SOURCE_LOCK_FALLBACK_MODE = intPreferencesKey("source_lock_fallback_mode")
        val SOURCE_LOCK_MAX_RETRIES = intPreferencesKey("source_lock_max_retries")
        val SOURCE_LOCK_RETRY_DELAY_MS = longPreferencesKey("source_lock_retry_delay_ms")
        val SOURCE_LOCK_PERSIST = booleanPreferencesKey("source_lock_persist")
        val SOURCE_LOCK_NOTIFY_FALLBACK = booleanPreferencesKey("source_lock_notify_fallback")
        val METADATA_PROVIDERS_ENABLED = stringPreferencesKey("metadata_providers_enabled")
    }
}
