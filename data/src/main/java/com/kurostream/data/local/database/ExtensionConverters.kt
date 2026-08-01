package com.kurostream.data.local.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.kurostream.domain.extension.ConfigField
import com.kurostream.domain.extension.ContentType
import com.kurostream.domain.extension.ExtensionCapability
import com.kurostream.domain.extension.ExtensionSourceFormat
import com.kurostream.domain.extension.ExtensionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
class ExtensionConverters(private val json: Json) {

    @TypeConverter
    fun fromExtensionType(value: ExtensionType): String = value.name

    @TypeConverter
    fun toExtensionType(value: String): ExtensionType = ExtensionType.valueOf(value)

    @TypeConverter
    fun fromContentTypes(value: Set<ContentType>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toContentTypes(value: String): Set<ContentType> = value.split(",").filter { it.isNotBlank() }.map { ContentType.valueOf(it) }.toSet()

    @TypeConverter
    fun fromCapabilities(value: Set<ExtensionCapability>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toCapabilities(value: String): Set<ExtensionCapability> = value.split(",").filter { it.isNotBlank() }.map { ExtensionCapability.valueOf(it) }.toSet()

    @TypeConverter
    fun fromSourceFormat(value: ExtensionSourceFormat): String = value.name

    @TypeConverter
    fun toSourceFormat(value: String): ExtensionSourceFormat = ExtensionSourceFormat.valueOf(value)

    @TypeConverter
    fun fromConfigFields(value: List<ConfigField>): String = json.encodeToString(value)

    @TypeConverter
    fun toConfigFields(value: String): List<ConfigField> = json.decodeFromString(value)
}
