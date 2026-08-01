package com.kurostream.data.local.entity

import com.kurostream.domain.extension.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun ExtensionEntity.toDomain(): UnifiedExtension {
    return UnifiedExtension(
        id = id,
        name = name,
        description = description,
        version = version,
        type = ExtensionType.valueOf(type),
        originUrl = originUrl,
        iconUrl = iconUrl,
        author = author,
        capabilities = capabilities.split(",").filter { it.isNotBlank() }.map { ExtensionCapability.valueOf(it) }.toSet(),
        supportedTypes = supportedTypes.split(",").filter { it.isNotBlank() }.map { ContentType.valueOf(it) }.toSet(),
        supportedLanguages = supportedLanguages.split(",").filter { it.isNotBlank() },
        sourceFormat = ExtensionSourceFormat.valueOf(sourceFormat),
        rawManifest = rawManifest,
        configSchema = try { Json.decodeFromString(configSchema) } catch (e: Exception) { emptyList() },
        healthScore = healthScore,
        isOfficial = isOfficial,
        isEnabled = isEnabled,
        isInstalled = isInstalled,
    )
}

fun UnifiedExtension.toEntity(): ExtensionEntity {
    return ExtensionEntity(
        id = id,
        name = name,
        description = description,
        version = version,
        type = type.name,
        originUrl = originUrl,
        iconUrl = iconUrl,
        author = author,
        capabilities = capabilities.joinToString(",") { it.name },
        supportedTypes = supportedTypes.joinToString(",") { it.name },
        supportedLanguages = supportedLanguages.joinToString(","),
        sourceFormat = sourceFormat.name,
        rawManifest = rawManifest,
        configSchema = Json.encodeToString(configSchema),
        healthScore = healthScore,
        isOfficial = isOfficial,
        isEnabled = isEnabled,
        isInstalled = isInstalled,
    )
}
