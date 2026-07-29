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

package com.kurostream.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["sourceId", "sourceType"]),
        Index(value = ["title"]),
        Index(value = ["category"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val category: String,
    val releaseDate: Long?,
    val rating: Double?,
    val duration: Long?,
    val streamUrl: String?,
    val metadataJson: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Fts4(contentEntity = MediaItemEntity::class)
@Entity(tableName = "media_items_fts")
data class MediaItemFts(
    val title: String,
)
