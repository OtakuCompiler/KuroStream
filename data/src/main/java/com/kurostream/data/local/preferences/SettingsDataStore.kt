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
import kotlinx.coroutines.flow.map

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

    // Convenience properties
    val syncEnabled: Flow<Boolean>
        get() = data.map { it[Keys.SYNC_ENABLED] ?: true }

    suspend fun setSyncEnabled(value: Boolean) = setBoolean(Keys.SYNC_ENABLED.name, value)

    val lastSyncTimestamp: Flow<Long>
        get() = data.map { it[Keys.LAST_SYNC_TIMESTAMP] ?: 0L }

    suspend fun setLastSyncTimestamp(value: Long) = setLong(Keys.LAST_SYNC_TIMESTAMP.name, value)

    val skinName: Flow<String>
        get() = data.map { it[Keys.SKIN_NAME] ?: "default" }

    suspend fun setSkinName(value: String) = setString(Keys.SKIN_NAME.name, value)

    val themeMode: Flow<String>
        get() = data.map { it[Keys.THEME_MODE] ?: "system" }

    suspend fun setThemeMode(value: String) = setString(Keys.THEME_MODE.name, value)

    val subtitleLanguage: Flow<String>
        get() = data.map { it[Keys.SUBTITLE_LANGUAGE] ?: "en" }

    suspend fun setSubtitleLanguage(value: String) = setString(Keys.SUBTITLE_LANGUAGE.name, value)

    val customHomeRows: Flow<String>
        get() = data.map { it[Keys.CUSTOM_HOME_ROWS] ?: "" }

    suspend fun setCustomHomeRows(value: String) = setString(Keys.CUSTOM_HOME_ROWS.name, value)

    object Keys {
        val SOURCE_LOCK_ENABLED = booleanPreferencesKey("source_lock_enabled")
        val SOURCE_LOCK_FALLBACK_MODE = intPreferencesKey("source_lock_fallback_mode")
        val SOURCE_LOCK_MAX_RETRIES = intPreferencesKey("source_lock_max_retries")
        val SOURCE_LOCK_RETRY_DELAY_MS = longPreferencesKey("source_lock_retry_delay_ms")
        val SOURCE_LOCK_PERSIST = booleanPreferencesKey("source_lock_persist")
        val SOURCE_LOCK_NOTIFY_FALLBACK = booleanPreferencesKey("source_lock_notify_fallback")
        val METADATA_PROVIDERS_ENABLED = stringPreferencesKey("metadata_providers_enabled")
        
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val SKIN_NAME = stringPreferencesKey("skin_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val CUSTOM_HOME_ROWS = stringPreferencesKey("custom_home_rows")
    }
}
