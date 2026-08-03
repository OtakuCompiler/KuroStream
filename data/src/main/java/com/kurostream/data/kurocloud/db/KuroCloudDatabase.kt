package com.kurostream.data.kurocloud.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kurostream.data.local.database.Converters

@Database(
    entities = [
        KuroEntitlementsEntity::class,
        KuroCatalogEntity::class,
        KuroPurchaseEntity::class,
        KuroCatalogMetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KuroCloudDatabase : RoomDatabase() {
    abstract fun entitlementsDao(): KuroEntitlementsDao
    abstract fun catalogDao(): KuroCatalogDao
    abstract fun purchaseDao(): KuroPurchaseDao
    abstract fun catalogMetaDao(): KuroCatalogMetaDao

    companion object {
        @Volatile private var INSTANCE: KuroCloudDatabase? = null

        fun getDatabase(context: Context): KuroCloudDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KuroCloudDatabase::class.java,
                    "kuro_cloud.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}