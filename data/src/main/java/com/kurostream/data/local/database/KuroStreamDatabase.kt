package com.kurostream.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kurostream.data.local.dao.AddonDao
import com.kurostream.data.local.dao.BookmarkDao
import com.kurostream.data.local.dao.ExtensionDao
import com.kurostream.data.local.dao.FavoriteDao
import com.kurostream.data.local.dao.HomeRowDao
import com.kurostream.data.local.dao.MediaItemDao
import com.kurostream.data.local.dao.ProfileDao
import com.kurostream.data.local.dao.PurchaseDao
import com.kurostream.data.local.dao.SourceLockDao
import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.data.local.entity.AddonConfigEntity
import com.kurostream.data.local.entity.BookmarkEntity
import com.kurostream.data.local.entity.ExtensionEntity
import com.kurostream.data.local.entity.FavoriteEntity
import com.kurostream.data.local.entity.HomeRowEntity
import com.kurostream.data.local.entity.MediaItemEntity
import com.kurostream.data.local.entity.ProfileEntity
import com.kurostream.data.local.entity.PurchaseEntity
import com.kurostream.data.local.entity.SourceLockEntity
import com.kurostream.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        MediaItemEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        ProfileEntity::class,
        SourceLockEntity::class,
        HomeRowEntity::class,
        BookmarkEntity::class,
        PurchaseEntity::class,
        AddonConfigEntity::class,
        ExtensionEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(ExtensionConverters::class, Converters::class)
abstract class KuroStreamDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun profileDao(): ProfileDao
    abstract fun sourceLockDao(): SourceLockDao
    abstract fun homeRowDao(): HomeRowDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun addonDao(): AddonDao
    abstract fun extensionDao(): ExtensionDao
}
