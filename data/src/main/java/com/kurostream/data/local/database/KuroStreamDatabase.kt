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

package com.kurostream.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kurostream.data.local.dao.*
import com.kurostream.data.local.entity.*

@Database(
    entities = [
        MediaItemEntity::class,
        MediaItemFts::class,
        ProfileEntity::class,
        WatchHistoryEntity::class,
        FavoriteEntity::class,
        DownloadItemEntity::class,
        SourceLockEntity::class,
        SourceLockSettingsEntity::class,
        SourceLockFallbackEntity::class,
        HomeRowEntity::class,
        BookmarkEntity::class,
        AddonConfigEntity::class,
        PurchaseEntity::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KuroStreamDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun profileDao(): ProfileDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadItemDao(): DownloadItemDao
    abstract fun sourceLockDao(): SourceLockDao
    abstract fun homeRowDao(): HomeRowDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun addonDao(): AddonDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        /**
         * WAL mode and PRAGMAs are configured via Room's setJournalMode in RoomModule.
         * Do NOT use execSQL for PRAGMAs as it conflicts with Room's connection management
         * and causes: "Queries can be performed using SQLiteDatabase query or rawQuery methods only"
         */
    }
}