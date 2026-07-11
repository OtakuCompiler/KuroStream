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

    suspend fun updateDataAsync(block: Preferences.() -> Unit)
}

import androidx.datastore.preferences.Preferences