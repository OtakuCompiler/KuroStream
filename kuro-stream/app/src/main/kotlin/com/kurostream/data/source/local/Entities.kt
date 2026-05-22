package com.kurostream.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.kurostream.core.plugin.PluginType

@Entity(tableName = "content")
data class ContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val poster: String?,
    val backdrop: String?,
    val year: Int?,
    val rating: Double?,
    val description: String?,
    val genres: String,
    val sourcePlugin: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val baseUrl: String,
    val manifestUrl: String,
    val version: String,
    val description: String,
    val isEnabled: Boolean,
    val logoUrl: String?,
    val supportedTypes: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val contentTitle: String,
    val poster: String?,
    val watchedAt: Long,
    val progressMs: Long = 0,
    val durationMs: Long = 0
)

class Converters {
    @TypeConverter
    fun listToString(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun stringToList(s: String): List<String> =
        if (s.isBlank()) emptyList() else s.split(",")

    @TypeConverter
    fun pluginTypeToString(type: PluginType): String = type.name

    @TypeConverter
    fun stringToPluginType(s: String): PluginType = PluginType.valueOf(s)
}
