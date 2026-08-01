package com.kurostream.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `source_lock_settings` (" +
                "`id` INTEGER NOT NULL, " +
                "`enabled` INTEGER NOT NULL, " +
                "`fallback_mode_ordinal` INTEGER NOT NULL, " +
                "`max_retries` INTEGER NOT NULL, " +
                "`retry_delay_ms` INTEGER NOT NULL, " +
                "`persist_across_sessions` INTEGER NOT NULL, " +
                "`notify_on_fallback` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `source_lock_fallbacks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`series_id` TEXT NOT NULL, " +
                "`from_provider` TEXT NOT NULL, " +
                "`to_provider` TEXT NOT NULL, " +
                "`reason` TEXT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `media_items_fts` USING FTS4(" +
                "`title` TEXT, content=`media_items`)"
        )
    }
}
