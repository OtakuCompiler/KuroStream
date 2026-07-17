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

package com.kurostream.data.metadata

import com.kurostream.core.common.result.Result
import com.kurostream.domain.metadata.*
import com.kurostream.domain.model.SourceLockSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedMetadataRepositoryImpl @Inject constructor(
    private val kitsuProvider: KitsuMetadataProvider,
    private val anilistProvider: AniListMetadataProvider,
    private val malProvider: MalMetadataProvider,
    private val tmdbProvider: TmdbMetadataProvider,
    private val tvdbProvider: TvdbMetadataProvider,
    private val imdbProvider: ImdbMetadataProvider,
    private val settingsDataStore: com.kurostream.data.local.preferences.SettingsDataStore,
) : UnifiedMetadataRepository {

    private val _enabledProviders = MutableStateFlow<Set<String>>(emptySet())
    val enabledProviders: kotlinx.coroutines.flow.StateFlow<Set<String>> = _enabledProviders

    private val allProviders = listOf(
        kitsuProvider, anilistProvider, malProvider, tmdbProvider, tvdbProvider, imdbProvider
    ).sortedBy { it.priority }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            _enabledProviders.value = getEnabledProvidersFromSettings()
        }
    }

    override suspend fun getAnimeDetails(id: String): MetadataResult<UnifiedAnimeDetails> = withContext(Dispatchers.IO) {
        val providerErrors = mutableMapOf<String, String>()
        var bestResult: UnifiedAnimeDetails? = null
        var missingProviders = mutableListOf<String>()

        for (provider in allProviders) {
            if (!provider.isEnabled || !_enabledProviders.value.contains(provider.providerId)) {
                missingProviders.add(provider.providerId)
                continue
            }

            try {
                val result = provider.getAnime(id)
                when (result) {
                    is MetadataResult.Success -> {
                        val unified = convertToUnified(result.data, provider.providerId)
                        if (bestResult == null || provider.priority < getProviderPriority(bestResult.providerData)) {
                            bestResult = unified
                        }
                    }
                    is MetadataResult.Error -> {
                        providerErrors[provider.providerId] = result.message
                    }
                    is MetadataResult.Partial -> {
                        val unified = convertToUnified(result.data, provider.providerId)
                        if (bestResult == null || provider.priority < getProviderPriority(bestResult.providerData)) {
                            bestResult = unified
                        }
                        providerErrors[provider.providerId] = result.providerErrors.values.joinToString(", ")
                    }
                    is MetadataResult.NotFound -> { /* provider returned not found, skip */ }
                    is MetadataResult.RateLimited -> {
                        providerErrors[provider.providerId] = "Rate limited, retry after ${result.retryAfterMs}ms"
                    }
                }
            } catch (e: Exception) {
                providerErrors[provider.providerId] = e.message ?: "Unknown error"
            }
        }

        return@withContext if (bestResult != null) {
            if (providerErrors.isNotEmpty() || missingProviders.isNotEmpty()) {
                MetadataResult.Partial(bestResult, missingProviders, providerErrors)
            } else {
                MetadataResult.Success(bestResult)
            }
        } else {
            MetadataResult.Error("No provider returned data", providerErrors = providerErrors)
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<UnifiedAnimeDetails>> = withContext(Dispatchers.IO) {
        val providerErrors = mutableMapOf<String, String>()
        val mergedResults = mutableMapOf<String, UnifiedAnimeDetails>()
        var missingProviders = mutableListOf<String>()

        for (provider in allProviders) {
            if (!provider.isEnabled || !_enabledProviders.value.contains(provider.providerId)) {
                missingProviders.add(provider.providerId)
                continue
            }

            try {
                val result = provider.searchAnime(query, page, limit)
                when (result) {
                    is MetadataResult.Success -> {
                        result.data.forEach { item ->
                            val unified = convertToUnified(item, provider.providerId)
                            val existing = mergedResults[unified.id]
                            if (existing == null || provider.priority < getProviderPriority(existing.providerData)) {
                                mergedResults[unified.id] = unified
                            }
                        }
                    }
                    is MetadataResult.Error -> {
                        providerErrors[provider.providerId] = result.message
                    }
                    is MetadataResult.Partial -> {
                        result.data.forEach { item ->
                            val unified = convertToUnified(item, provider.providerId)
                            val existing = mergedResults[unified.id]
                            if (existing == null || provider.priority < getProviderPriority(existing.providerData)) {
                                mergedResults[unified.id] = unified
                            }
                        }
                        providerErrors[provider.providerId] = result.providerErrors.values.joinToString(", ")
                    }
                    is MetadataResult.NotFound -> { /* skip */ }
                    is MetadataResult.RateLimited -> {
                        providerErrors[provider.providerId] = "Rate limited, retry after ${result.retryAfterMs}ms"
                    }
                }
            } catch (e: Exception) {
                providerErrors[provider.providerId] = e.message ?: "Unknown error"
            }
        }

        val results = mergedResults.values.toList()
            .sortedByDescending { it.score ?: 0.0 }
            .take(limit)

        return@withContext if (results.isNotEmpty()) {
            if (providerErrors.isNotEmpty() || missingProviders.isNotEmpty()) {
                MetadataResult.Partial(results, missingProviders, providerErrors)
            } else {
                MetadataResult.Success(results)
            }
        } else {
            MetadataResult.Error("No results found", providerErrors = providerErrors)
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<UnifiedAnimeDetails>> = withContext(Dispatchers.IO) {
        val providerErrors = mutableMapOf<String, String>()
        val mergedResults = mutableMapOf<String, UnifiedAnimeDetails>()
        var missingProviders = mutableListOf<String>()

        for (provider in allProviders) {
            if (!provider.isEnabled || !_enabledProviders.value.contains(provider.providerId)) {
                missingProviders.add(provider.providerId)
                continue
            }

            try {
                val result = provider.getSeasonalAnime(year, season)
                when (result) {
                    is MetadataResult.Success -> {
                        result.data.forEach { item ->
                            val unified = convertToUnified(item, provider.providerId)
                            val existing = mergedResults[unified.id]
                            if (existing == null || provider.priority < getProviderPriority(existing.providerData)) {
                                mergedResults[unified.id] = unified
                            }
                        }
                    }
                    is MetadataResult.Error -> {
                        providerErrors[provider.providerId] = result.message
                    }
                    is MetadataResult.Partial -> {
                        result.data.forEach { item ->
                            val unified = convertToUnified(item, provider.providerId)
                            val existing = mergedResults[unified.id]
                            if (existing == null || provider.priority < getProviderPriority(existing.providerData)) {
                                mergedResults[unified.id] = unified
                            }
                        }
                        providerErrors[provider.providerId] = result.providerErrors.values.joinToString(", ")
                    }
                    is MetadataResult.NotFound -> { /* skip */ }
                    is MetadataResult.RateLimited -> {
                        providerErrors[provider.providerId] = "Rate limited, retry after ${result.retryAfterMs}ms"
                    }
                }
            } catch (e: Exception) {
                providerErrors[provider.providerId] = e.message ?: "Unknown error"
            }
        }

        val results = mergedResults.values.toList()
            .sortedByDescending { it.popularity ?: 0 }

        return@withContext if (results.isNotEmpty()) {
            if (providerErrors.isNotEmpty() || missingProviders.isNotEmpty()) {
                MetadataResult.Partial(results, missingProviders, providerErrors)
            } else {
                MetadataResult.Success(results)
            }
        } else {
            MetadataResult.Error("No seasonal anime found", providerErrors = providerErrors)
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<UnifiedAnimeDetails>> = withContext(Dispatchers.IO) {
        val providerErrors = mutableMapOf<String, String>()
        val mergedResults = mutableMapOf<String, UnifiedAnimeDetails>()
        var missingProviders = mutableListOf<String>()

        for (provider in allProviders) {
            if (!provider.isEnabled || !_enabledProviders.value.contains(provider.providerId)) {
                missingProviders.add(provider.providerId)
                continue
            }

            try {
                val result = provider.getTrendingAnime(limit)
                when (result) {
                    is MetadataResult.Success -> {
                        result.data.forEach { item ->
                            val unified = convertToUnified(item, provider.providerId)
                            val existing = mergedResults[unified.id]
                            if (existing == null || provider.priority < getProviderPriority(existing.providerData)) {
                                mergedResults[unified.id] = unified
                            }
                        }
                    }
                    is MetadataResult.Error -> {
                        providerErrors[provider.providerId] = result.message
                    }
                    is MetadataResult.Partial -> {
                        result.data.forEach { item ->
                            val unified = convertToUnified(item, provider.providerId)
                            val existing = mergedResults[unified.id]
                            if (existing == null || provider.priority < getProviderPriority(existing.providerData)) {
                                mergedResults[unified.id] = unified
                            }
                        }
                        providerErrors[provider.providerId] = result.providerErrors.values.joinToString(", ")
                    }
                    is MetadataResult.NotFound -> { /* skip */ }
                    is MetadataResult.RateLimited -> {
                        providerErrors[provider.providerId] = "Rate limited, retry after ${result.retryAfterMs}ms"
                    }
                }
            } catch (e: Exception) {
                providerErrors[provider.providerId] = e.message ?: "Unknown error"
            }
        }

        val results = mergedResults.values.toList()
            .sortedByDescending { it.score ?: 0.0 }
            .take(limit)

        return@withContext if (results.isNotEmpty()) {
            if (providerErrors.isNotEmpty() || missingProviders.isNotEmpty()) {
                MetadataResult.Partial(results, missingProviders, providerErrors)
            } else {
                MetadataResult.Success(results)
            }
        } else {
            MetadataResult.Error("No trending anime found", providerErrors = providerErrors)
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<UnifiedAnimeDetails> = withContext(Dispatchers.IO) {
        for (provider in allProviders) {
            if (!provider.isEnabled || !_enabledProviders.value.contains(provider.providerId)) continue

            try {
                val result = provider.getAnimeByExternalId(type, value)
                if (result is MetadataResult.Success) {
                    return@withContext MetadataResult.Success(convertToUnified(result.data, provider.providerId))
                }
            } catch (e: Exception) {
                // Try next provider
            }
        }
        MetadataResult.Error("No provider found anime with external ID")
    }

    override fun observeEnabledProviders(): kotlinx.coroutines.flow.Flow<List<MetadataProvider>> {
        return _enabledProviders.map { enabledSet ->
            allProviders.filter { it.providerId in enabledSet }
        }
    }

    override suspend fun setProviderEnabled(providerId: String, enabled: Boolean) {
        val current = _enabledProviders.value.toMutableSet()
        if (enabled) current.add(providerId) else current.remove(providerId)
        _enabledProviders.value = current
        saveEnabledProvidersToSettings(current)
    }

    override suspend fun setProviderPriority(providerId: String, priority: Int) {
        // Implementation would update provider priorities in settings
        // For now, providers are sorted by their default priority
    }

    private fun convertToUnified(data: AnimeMetadata, sourceProviderId: String): UnifiedAnimeDetails {
        val providerData = mutableMapOf<String, String>()
        providerData["_source"] = sourceProviderId
        providerData["_priority"] = (allProviders.find { it.providerId == sourceProviderId }?.priority ?: 999).toString()

        return UnifiedAnimeDetails(
            id = data.id,
            title = data.title,
            titleEnglish = data.titleEnglish,
            titleJapanese = data.titleJapanese,
            synonyms = data.synonyms,
            description = data.description,
            coverImageUrl = data.coverImageUrl,
            bannerImageUrl = data.bannerImageUrl,
            type = data.type,
            status = data.status,
            startDate = data.startDate,
            endDate = data.endDate,
            season = data.season,
            seasonYear = data.seasonYear,
            genres = data.genres,
            studios = data.studios,
            score = data.score,
            scoredBy = data.scoredBy,
            rank = data.rank,
            popularity = data.popularity,
            favorites = data.favorites,
            ageRating = data.ageRating,
            sourceMaterial = data.sourceMaterial,
            durationMinutes = data.durationMinutes,
            episodeCount = data.episodes,
            trailerUrl = data.trailerUrl,
            externalLinks = data.externalLinks.map { ExternalLink(it.site, it.url) },
            characters = data.characters.map { Character(it.id, it.name, it.role, it.imageUrl) },
            staff = data.staff.map { Staff(it.id, it.name, it.role, it.imageUrl) },
            relations = data.relations.map { AnimeRelation(it.relationType, it.relatedAnimeId, it.relatedTitle, it.targetId, it.targetTitle, it.targetType) },
            themes = data.themes,
            statistics = data.statistics?.let { AnimeStatistics(scoreDistribution = it.scoreDistribution, statusDistribution = it.statusDistribution, totalMembers = it.totalMembers, totalFavorites = it.totalFavorites) },
            providerData = providerData,
        )
    }

    private fun getProviderPriority(providerData: Map<String, String>): Int {
        return providerData["_priority"]?.toIntOrNull() ?: 999
    }

    private suspend fun getEnabledProvidersFromSettings(): Set<String> {
        return settingsDataStore.data
            .map { prefs ->
                val enabled = prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.METADATA_PROVIDERS_ENABLED] ?: "kitsu,anilist,mal,tmdb"
                enabled.split(",").toSet()
            }
            .first()
    }

    private suspend fun saveEnabledProvidersToSettings(providers: Set<String>) {
        settingsDataStore.editPreferences {
            this[com.kurostream.data.local.preferences.SettingsDataStore.Keys.METADATA_PROVIDERS_ENABLED] = providers.joinToString(",")
        }
    }
}
