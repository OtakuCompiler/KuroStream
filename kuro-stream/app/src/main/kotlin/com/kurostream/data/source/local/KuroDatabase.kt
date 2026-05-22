package com.kurostream.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ContentEntity::class, PluginEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KuroDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun pluginDao(): PluginDao
    abstract fun historyDao(): HistoryDao
}
