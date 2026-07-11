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

package com.kurostream.extensions.torrserver

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrServerConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("torrserver", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", "http://127.0.0.1:8090") ?: "http://127.0.0.1:8090"
        set(value) = prefs.edit { putString("server_url", value) }

    var cacheSize: Int
        get() = prefs.getInt("cache_size", 200)
        set(value) = prefs.edit { putInt("cache_size", value) }

    var preloadBuffer: Int
        get() = prefs.getInt("preload_buffer", 32)
        set(value) = prefs.edit { putInt("preload_buffer", value) }

    var readerAHead: Int
        get() = prefs.getInt("reader_ahead", 32)
        set(value) = prefs.edit { putInt("reader_ahead", value) }

    var useDiskCache: Boolean
        get() = prefs.getBoolean("use_disk_cache", true)
        set(value) = prefs.edit { putBoolean("use_disk_cache", value) }

    var removeAfterStop: Boolean
        get() = prefs.getBoolean("remove_after_stop", false)
        set(value) = prefs.edit { putBoolean("remove_after_stop", value) }

    var connectionsLimit: Int
        get() = prefs.getInt("connections_limit", 50)
        set(value) = prefs.edit { putInt("connections_limit", value) }

    var dhtConnectionLimit: Int
        get() = prefs.getInt("dht_limit", 500)
        set(value) = prefs.edit { putInt("dht_limit", value) }

    var isEnabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit { putBoolean("enabled", value) }

    fun getSettingsPayload(): Map<String, Any> = mapOf(
        "CacheSize" to cacheSize,
        "PreloadBufferSize" to preloadBuffer,
        "ReaderReadAHead" to readerAHead,
        "UseDisk" to useDiskCache,
        "RemoveCacheOnDrop" to removeAfterStop,
        "ConnectionsLimit" to connectionsLimit,
        "DhtConnectionLimit" to dhtConnectionLimit
    )
}
