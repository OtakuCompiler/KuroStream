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

package com.kurostream.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    fun observeDynamicColorsEnabled(): Flow<Boolean>
    suspend fun setDynamicColorsEnabled(enabled: Boolean)
    fun observeAutoUpdateExtensions(): Flow<Boolean>
    suspend fun setAutoUpdateExtensions(enabled: Boolean)
    fun observeDefaultQuality(): Flow<String>
    suspend fun setDefaultQuality(quality: String)
    fun observeSkipIntroEnabled(): Flow<Boolean>
    suspend fun setSkipIntroEnabled(enabled: Boolean)
    fun observeSkipOutroEnabled(): Flow<Boolean>
    suspend fun setSkipOutroEnabled(enabled: Boolean)
    fun observeCacheSizeMb(): Flow<Int>
    suspend fun setCacheSizeMb(size: Int)
    suspend fun clearAllSettings()
}

enum class AppTheme { SYSTEM, LIGHT, DARK, OLED }
