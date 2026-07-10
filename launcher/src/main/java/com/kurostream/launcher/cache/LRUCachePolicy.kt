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

import com.kurostream.launcher.data.local.entity.CacheEntryEntity
import java.io.File

class LRUCachePolicy : CacheEvictionPolicy {

    override fun selectForEviction(entries: List<CacheEntryEntity>, targetSize: Long): List<CacheEntryEntity> {
        val completedEntries = entries.filter { it.status == CacheStatus.COMPLETED }
            .sortedBy { it.lastAccessed } // Least recently used first

        val toEvict = mutableListOf<CacheEntryEntity>()
        var freedSize = 0L

        for (entry in completedEntries) {
            if (freedSize >= targetSize) break

            val file = File(entry.localPath)
            if (file.exists()) {
                toEvict.add(entry)
                freedSize += file.length()
            }
        }

        return toEvict
    }
}
