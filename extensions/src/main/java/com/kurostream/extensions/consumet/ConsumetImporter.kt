package com.kurostream.extensions.consumet

import com.kurostream.domain.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumetImporter @Inject constructor(
    private val adapter: ConsumetAdapter,
) {

    suspend fun importFromUrl(baseUrl: String, name: String): Result<UnifiedExtension> {
        return try {
            val result = adapter.fetchAnime(baseUrl, "test")
            result.map { anime ->
                adapter.toUnifiedExtension(
                    name = name.ifBlank { baseUrl.removeSuffix("/") },
                    baseUrl = baseUrl,
                    description = anime.description ?: "Consumet anime source",
                    iconUrl = anime.image ?: "",
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to import Consumet source: $baseUrl")
            Result.failure(e)
        }
    }

    suspend fun importFromPreset(preset: ConsumetPreset): Result<UnifiedExtension> {
        return importFromUrl(preset.baseUrl, preset.name)
    }
}

data class ConsumetPreset(
    val name: String,
    val baseUrl: String,
    val description: String = "",
    val iconUrl: String = "",
    val author: String = "",
)

object ConsumetPresets {
    val ANIKKU = ConsumetPreset(
        name = "Anikku",
        baseUrl = "https://api.anikku.org/v1",
        description = "Anikku anime streaming extension",
    )
    val HAYASE = ConsumetPreset(
        name = "Hayase",
        baseUrl = "https://hayase-consumet.vercel.app/api",
        description = "Hayase anime streaming extension",
    )
    val CONSUMET_DEFAULT = ConsumetPreset(
        name = "Consumet Default",
        baseUrl = "https://consumet.org/api/v1",
        description = "Default Consumet anime API",
    )
}
