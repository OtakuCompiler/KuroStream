package com.kurostream.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extensions")
data class ExtensionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val type: String,
    val originUrl: String,
    val iconUrl: String,
    val author: String,
    val capabilities: String,
    val supportedTypes: String,
    val supportedLanguages: String,
    val sourceFormat: String,
    val rawManifest: String,
    val configSchema: String,
    val healthScore: Float,
    val isOfficial: Boolean,
    val isEnabled: Boolean,
    val isInstalled: Boolean,
    val installedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
)
