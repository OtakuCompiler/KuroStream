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
    suspend fun getString(key: Preferences.Key<String>, default: String): String
    suspend fun setString(key: Preferences.Key<String>, value: String)

    suspend fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean): Boolean
    suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean)

    suspend fun getInt(key: Preferences.Key<Int>, default: Int): Int
    suspend fun setInt(key: Preferences.Key<Int>, value: Int)

    suspend fun getLong(key: Preferences.Key<Long>, default: Long): Long
    suspend fun setLong(key: Preferences.Key<Long>, value: Long)

    suspend fun getFloat(key: Preferences.Key<Float>, default: Float): Float
    suspend fun setFloat(key: Preferences.Key<Float>, value: Float)

    val data: Flow<Preferences>

    suspend fun editPreferences(block: suspend MutablePreferences.() -> Unit)

    val syncEnabled: Flow<Boolean>
        get() = data.map { it[Keys.SYNC_ENABLED] ?: true }

    suspend fun setSyncEnabled(value: Boolean) = setBoolean(Keys.SYNC_ENABLED, value)

    val lastSyncTimestamp: Flow<Long>
        get() = data.map { it[Keys.LAST_SYNC_TIMESTAMP] ?: 0L }

    suspend fun setLastSyncTimestamp(value: Long) = setLong(Keys.LAST_SYNC_TIMESTAMP, value)

    val skinName: Flow<String>
        get() = data.map { it[Keys.SKIN_NAME] ?: "default" }

    suspend fun setSkinName(value: String) = setString(Keys.SKIN_NAME, value)

    val themeMode: Flow<String>
        get() = data.map { it[Keys.THEME_MODE] ?: "system" }

    suspend fun setThemeMode(value: String) = setString(Keys.THEME_MODE, value)

    val subtitleLanguage: Flow<String>
        get() = data.map { it[Keys.SUBTITLE_LANGUAGE] ?: "en" }

    suspend fun setSubtitleLanguage(value: String) = setString(Keys.SUBTITLE_LANGUAGE, value)

    val customHomeRows: Flow<String>
        get() = data.map { it[Keys.CUSTOM_HOME_ROWS] ?: "" }

    suspend fun setCustomHomeRows(value: String) = setString(Keys.CUSTOM_HOME_ROWS, value)

    val userRegion: Flow<String>
        get() = data.map { it[Keys.USER_REGION] ?: "US" }

    suspend fun setUserRegion(value: String) = setString(Keys.USER_REGION, value)

    val userContentPreference: Flow<String>
        get() = data.map { it[Keys.USER_CONTENT_PREFERENCE] ?: "all" }

    suspend fun setUserContentPreference(value: String) = setString(Keys.USER_CONTENT_PREFERENCE, value)

    val onboardingCompleted: Flow<Boolean>
        get() = data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(value: Boolean) = setBoolean(Keys.ONBOARDING_COMPLETED, value)

    object Keys {
        val SOURCE_LOCK_ENABLED = booleanPreferencesKey("source_lock_enabled")
        val SOURCE_LOCK_FALLBACK_MODE = intPreferencesKey("source_lock_fallback_mode")
        val SOURCE_LOCK_MAX_RETRIES = intPreferencesKey("source_lock_max_retries")
        val SOURCE_LOCK_RETRY_DELAY_MS = longPreferencesKey("source_lock_retry_delay_ms")
        val SOURCE_LOCK_PERSIST = booleanPreferencesKey("source_lock_persist")
        val SOURCE_LOCK_NOTIFY_FALLBACK = booleanPreferencesKey("source_lock_notify_fallback")
        val METADATA_PROVIDERS_ENABLED = stringPreferencesKey("metadata_providers_enabled")
        val USER_REGION = stringPreferencesKey("user_region")
        val USER_CONTENT_PREFERENCE = stringPreferencesKey("user_content_preference")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val SKIN_NAME = stringPreferencesKey("skin_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val CUSTOM_HOME_ROWS = stringPreferencesKey("custom_home_rows")
    }
}
