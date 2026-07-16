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
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["isActive"])]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String?,
    val pinHash: String?,
    val isActive: Boolean = false,
    val isPremium: Boolean = false,
    val preferredLanguage: String = "en",
    val preferredSubtitleLanguage: String = "en",
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false,
    val preferredQuality: String = "AUTO",
    val hasPin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val preferencesJson: String? = null
)
