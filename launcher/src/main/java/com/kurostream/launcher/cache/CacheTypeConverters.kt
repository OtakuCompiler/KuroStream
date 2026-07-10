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

package com.kurostream.launcher.cache

import androidx.room.TypeConverter

class CacheTypeConverters {
    @TypeConverter
    fun fromCacheStatus(status: CacheStatus): String = status.name

    @TypeConverter
    fun toCacheStatus(name: String): CacheStatus = CacheStatus.valueOf(name)

    @TypeConverter
    fun fromCachePriority(priority: CachePriority): String = priority.name

    @TypeConverter
    fun toCachePriority(name: String): CachePriority = CachePriority.valueOf(name)
}
