package com.kurostream.tv.data.repository

import com.kurostream.tv.data.local.database.CachedAnimeDao
import com.kurostream.tv.data.local.database.CachedAnimeEntity
import com.kurostream.tv.data.local.database.FavoriteDao
import com.kurostream.tv.data.local.database.FavoriteEntity
import com.kurostream.tv.data.local.database.WatchHistoryDao
import com.kurostream.tv.data.local.database.WatchHistoryEntity
import com.kurostream.tv.data.local.database.WatchProgressDao
import com.kurostream.tv.data.local.database.WatchProgressEntity
import com.kurostream.tv.data.metadata.AIOMetadataSystem
import com.kurostream.tv.data.metadata.EnrichedMetadata
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.domain.model.AnimeSeason
import com.kurostream.tv.domain.model.AnimeSource
import com.kurostream.tv.domain.model.AnimeStatus
import com.kurostream.tv.domain.model.Episode
import com.kurostream.tv.domain.model.StreamSource
import com.kurostream.tv.domain.model.StreamType
import com.kurostream.tv.domain.model.WatchProgress
import com.kurostream.tv.domain.model.WatchStatus
import com.kurostream.tv.domain.provider.ProviderAggregator
import com.kurostream.tv.domain.provider.ProviderEpisode
import com.kurostream.tv.domain.provider.ProviderSearchResult
import com.kurostream.tv.domain.repository.AnimeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AnimeRepository].
 *
 * Data hierarchy (fastest → freshest):
 *  1. Room [CachedAnimeDao] (local, TTL = 6 h)
 *  2. [AIOMetadataSystem] → Jikan v4 / AniList
 *  3. [ProviderAggregator] → Stremio / CloudStream (for streams & episodes)
 *
 * User-library state (favorites, watch progress, history) is stored
 * exclusively in Room and never network-fetched.
 */
@Singleton
class AnimeRepositoryImpl @Inject constructor(
    private val providerAggregator: ProviderAggregator,
    private val aioMetadata: AIOMetadataSystem,
    private val watchProgressDao: WatchProgressDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val cachedAnimeDao: CachedAnimeDao,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AnimeRepository {

    companion object {
        private const val TAG = "AnimeRepository"
        private const val JIKAN_BASE = "https://api.jikan.moe/v4"
        private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
    }

    // ─── Discovery ────────────────────────────────────────────────────────────

    override fun searchAnime(query: String, page: Int, perPage: Int): Flow<Result<List<Anime>>> =
        flow {
            try {
                val results = providerAggregator.search(query, page, maxProviders = 3)
                emit(Result.success(results.map { it.toAnimeDomain() }))
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "searchAnime query=%s", query)
                emit(Result.failure(e))
            }
        }.flowOn(ioDispatcher)

    override fun getTrendingAnime(page: Int, perPage: Int): Flow<Result<List<Anime>>> = flow {
        try {
            emit(Result.success(fetchJikanTopAnime("airing", page, perPage)))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getTrendingAnime")
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getPopularAnime(page: Int, perPage: Int): Flow<Result<List<Anime>>> = flow {
        try {
            emit(Result.success(fetchJikanTopAnime("bypopularity", page, perPage)))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getPopularAnime")
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getSeasonalAnime(
        year: Int,
        season: AnimeSeason,
        page: Int,
        perPage: Int
    ): Flow<Result<List<Anime>>> = flow {
        try {
            val seasonStr = season.name.lowercase()
            val url = "$JIKAN_BASE/seasons/$year/$seasonStr?page=$page&limit=$perPage"
            emit(Result.success(fetchJikanAnimeList(url)))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getSeasonalAnime year=%d season=%s", year, season)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getRecentlyUpdated(page: Int, perPage: Int): Flow<Result<List<Anime>>> = flow {
        try {
            emit(Result.success(fetchJikanTopAnime("airing", page, perPage)))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getRecentlyUpdated")
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getRecommendations(animeId: String, limit: Int): Flow<Result<List<Anime>>> =
        flow {
            try {
                val malId = animeId.toLongOrNull()
                if (malId != null) {
                    val url = "$JIKAN_BASE/anime/$malId/recommendations"
                    emit(Result.success(fetchJikanRecommendations(url, limit)))
                } else {
                    emit(Result.success(emptyList()))
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "getRecommendations animeId=%s", animeId)
                emit(Result.failure(e))
            }
        }.flowOn(ioDispatcher)

    // ─── Anime Details ────────────────────────────────────────────────────────

    override fun getAnimeById(id: String): Flow<Result<Anime>> = flow {
        try {
            val cached = cachedAnimeDao.getById(id)
            if (cached != null && System.currentTimeMillis() - cached.cachedAt < CACHE_TTL_MS) {
                emit(Result.success(cached.toDomain()))
                return@flow
            }
            val malId = id.toLongOrNull()
            val unifiedId = aioMetadata.resolveIds(
                malId = malId,
                title = if (malId == null) id else null
            )
            val enriched = aioMetadata.enrich(unifiedId)
            val anime = enriched.toDomain(id)
            cachedAnimeDao.insert(anime.toEntity())
            emit(Result.success(anime))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getAnimeById id=%s", id)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getAnimeByAnilistId(anilistId: Int): Flow<Result<Anime>> = flow {
        try {
            val unifiedId = aioMetadata.resolveIds(anilistId = anilistId.toLong())
            val enriched = aioMetadata.enrich(unifiedId)
            val anime = enriched.toDomain(anilistId.toString())
            cachedAnimeDao.insert(anime.toEntity())
            emit(Result.success(anime))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getAnimeByAnilistId anilistId=%d", anilistId)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getAnimeByMalId(malId: Int): Flow<Result<Anime>> = flow {
        try {
            val cached = cachedAnimeDao.getByMalId(malId.toLong())
            if (cached != null) {
                emit(Result.success(cached.toDomain()))
                return@flow
            }
            val unifiedId = aioMetadata.resolveIds(malId = malId.toLong())
            val enriched = aioMetadata.enrich(unifiedId)
            val anime = enriched.toDomain(malId.toString())
            cachedAnimeDao.insert(anime.toEntity())
            emit(Result.success(anime))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getAnimeByMalId malId=%d", malId)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    // ─── Episodes ─────────────────────────────────────────────────────────────

    override fun getEpisodes(animeId: String): Flow<Result<List<Episode>>> = flow {
        try {
            val providerEpisodes = providerAggregator.getEpisodes(animeId)
            emit(Result.success(providerEpisodes.map { it.toDomain(animeId) }))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getEpisodes animeId=%s", animeId)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getEpisode(animeId: String, episodeNumber: Int): Flow<Result<Episode>> = flow {
        try {
            val episodes = providerAggregator.getEpisodes(animeId)
            val episode = episodes.find { it.number == episodeNumber }
                ?: episodes.getOrNull(episodeNumber - 1)
            if (episode != null) {
                emit(Result.success(episode.toDomain(animeId)))
            } else {
                emit(Result.failure(NoSuchElementException("Episode $episodeNumber not found")))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getEpisode animeId=%s ep=%d", animeId, episodeNumber)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override fun getStreamSources(
        animeId: String,
        episodeNumber: Int
    ): Flow<Result<List<StreamSource>>> = flow {
        try {
            val providerStreams = providerAggregator.getStreams(animeId, episodeNumber.toString())
            val sources = providerStreams.map { ps ->
                StreamSource(
                    id = "${ps.providerId}:${ps.url.hashCode()}",
                    url = ps.url,
                    provider = ps.providerName,
                    quality = ps.quality.toDomainQuality(),
                    type = ps.type.toDomainType(),
                    headers = ps.headers,
                    isWorking = ps.isWorking
                )
            }
            emit(Result.success(sources))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getStreamSources animeId=%s ep=%d", animeId, episodeNumber)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    // ─── User Library ─────────────────────────────────────────────────────────

    override fun getMyList(status: WatchStatus?): Flow<Result<List<AnimeListEntry>>> = flow {
        try {
            val favorites = favoriteDao.getAllFavorites()
            val entries = favorites.mapNotNull { fav ->
                val progressList = watchProgressDao.getAllProgressForAnime(fav.animeId)
                val latestProgress = progressList.maxByOrNull { it.updatedAt }
                val anime = cachedAnimeDao.getById(fav.animeId)?.toDomain()
                    ?: Anime(id = fav.animeId, title = fav.title, coverImage = fav.coverImage)
                val watchStatus = fav.status?.let { runCatching { WatchStatus.valueOf(it) }.getOrNull() }
                    ?: WatchStatus.PLAN_TO_WATCH
                if (status != null && status != watchStatus) return@mapNotNull null
                val watchProg = WatchProgress(
                    animeId = fav.animeId,
                    currentEpisode = latestProgress?.episodeNumber ?: 0,
                    currentPosition = latestProgress?.positionMs ?: 0L,
                    totalEpisodes = fav.totalEpisodes ?: 0,
                    lastWatchedAt = latestProgress?.updatedAt ?: fav.addedAt,
                    status = watchStatus
                )
                AnimeListEntry(
                    anime = anime,
                    progress = watchProg,
                    addedAt = fav.addedAt,
                    updatedAt = latestProgress?.updatedAt ?: fav.addedAt
                )
            }
            emit(Result.success(entries))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getMyList")
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override suspend fun addToMyList(
        animeId: String,
        status: WatchStatus
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val cached = cachedAnimeDao.getById(animeId)
            favoriteDao.insert(
                FavoriteEntity(
                    animeId = animeId,
                    title = cached?.title ?: animeId,
                    coverImage = cached?.coverImage,
                    addedAt = System.currentTimeMillis(),
                    totalEpisodes = cached?.totalEpisodes,
                    status = status.name,
                    rating = cached?.rating
                )
            )
        }
    }

    override suspend fun removeFromMyList(animeId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { favoriteDao.deleteById(animeId) }
        }

    override suspend fun updateWatchStatus(
        animeId: String,
        status: WatchStatus
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            favoriteDao.getFavorite(animeId)?.let { existing ->
                favoriteDao.insert(existing.copy(status = status.name))
            }
        }
    }

    // ─── Watch Progress ───────────────────────────────────────────────────────

    override fun getWatchProgress(animeId: String): Flow<Result<WatchProgress?>> = flow {
        try {
            val progressList = watchProgressDao.getAllProgressForAnime(animeId)
            val latest = progressList.maxByOrNull { it.updatedAt }
            if (latest != null) {
                emit(
                    Result.success(
                        WatchProgress(
                            animeId = animeId,
                            currentEpisode = latest.episodeNumber,
                            currentPosition = latest.positionMs,
                            totalEpisodes = progressList.size,
                            lastWatchedAt = latest.updatedAt,
                            status = if (latest.isCompleted) WatchStatus.COMPLETED else WatchStatus.WATCHING
                        )
                    )
                )
            } else {
                emit(Result.success(null))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getWatchProgress animeId=%s", animeId)
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override suspend fun updateWatchProgress(
        animeId: String,
        episodeNumber: Int,
        position: Long,
        duration: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val isCompleted = duration > 0 && position.toFloat() / duration >= 0.9f
            watchProgressDao.insert(
                WatchProgressEntity(
                    animeId = animeId,
                    episodeNumber = episodeNumber,
                    positionMs = position,
                    durationMs = duration,
                    updatedAt = System.currentTimeMillis(),
                    isCompleted = isCompleted
                )
            )
        }
    }

    override suspend fun markEpisodeWatched(
        animeId: String,
        episodeNumber: Int
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            watchProgressDao.markCompleted(animeId, episodeNumber)
            val animeEntity = cachedAnimeDao.getById(animeId)
            watchHistoryDao.insert(
                WatchHistoryEntity(
                    id = "$animeId:$episodeNumber:${System.currentTimeMillis()}",
                    animeId = animeId,
                    animeTitle = animeEntity?.title ?: animeId,
                    episodeNumber = episodeNumber,
                    episodeTitle = null,
                    coverImage = animeEntity?.coverImage,
                    watchedAt = System.currentTimeMillis(),
                    watchDurationMs = 0L,
                    totalDurationMs = 0L,
                    lastPositionMs = 0L,
                    isCompleted = true
                )
            )
        }
    }

    override suspend fun markEpisodeUnwatched(
        animeId: String,
        episodeNumber: Int
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            watchProgressDao.getProgress(animeId, episodeNumber)?.let { existing ->
                watchProgressDao.insert(existing.copy(isCompleted = false, positionMs = 0L))
            }
        }
    }

    override fun getContinueWatching(limit: Int): Flow<Result<List<AnimeListEntry>>> = flow {
        try {
            val inProgress = watchProgressDao.getContinueWatching(limit)
            val entries = inProgress.mapNotNull { prog ->
                val anime = cachedAnimeDao.getById(prog.animeId)?.toDomain()
                    ?: return@mapNotNull null
                val watchProg = WatchProgress(
                    animeId = prog.animeId,
                    currentEpisode = prog.episodeNumber,
                    currentPosition = prog.positionMs,
                    totalEpisodes = 0,
                    lastWatchedAt = prog.updatedAt,
                    status = WatchStatus.WATCHING
                )
                AnimeListEntry(
                    anime = anime,
                    progress = watchProg,
                    addedAt = prog.updatedAt,
                    updatedAt = prog.updatedAt
                )
            }
            emit(Result.success(entries))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getContinueWatching")
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    // ─── Favorites ────────────────────────────────────────────────────────────

    override fun getFavorites(): Flow<Result<List<Anime>>> = flow {
        try {
            val favorites = favoriteDao.getAllFavorites()
            val anime = favorites.map { fav ->
                cachedAnimeDao.getById(fav.animeId)?.toDomain()
                    ?: Anime(
                        id = fav.animeId,
                        title = fav.title,
                        coverImage = fav.coverImage,
                        rating = fav.rating
                    )
            }
            emit(Result.success(anime))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getFavorites")
            emit(Result.failure(e))
        }
    }.flowOn(ioDispatcher)

    override suspend fun addToFavorites(animeId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val cached = cachedAnimeDao.getById(animeId)
                favoriteDao.insert(
                    FavoriteEntity(
                        animeId = animeId,
                        title = cached?.title ?: animeId,
                        coverImage = cached?.coverImage,
                        addedAt = System.currentTimeMillis(),
                        totalEpisodes = cached?.totalEpisodes,
                        status = null,
                        rating = cached?.rating
                    )
                )
            }
        }

    override suspend fun removeFromFavorites(animeId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { favoriteDao.deleteById(animeId) }
        }

    override fun isFavorite(animeId: String): Flow<Boolean> = flow {
        emit(favoriteDao.isFavorite(animeId))
    }.flowOn(ioDispatcher)

    // ─── Cache ────────────────────────────────────────────────────────────────

    override suspend fun clearCache(): Unit = withContext(ioDispatcher) {
        cachedAnimeDao.clearAll()
    }

    override suspend fun refreshAnime(animeId: String): Result<Anime> =
        withContext(ioDispatcher) {
            runCatching {
                val malId = animeId.toLongOrNull()
                val unifiedId = aioMetadata.resolveIds(
                    malId = malId,
                    title = if (malId == null) animeId else null
                )
                val enriched = aioMetadata.enrich(unifiedId)
                val anime = enriched.toDomain(animeId)
                cachedAnimeDao.insert(anime.toEntity())
                anime
            }
        }

    // ─── Jikan helpers ────────────────────────────────────────────────────────

    private suspend fun fetchJikanTopAnime(
        filter: String,
        page: Int,
        limit: Int
    ): List<Anime> {
        val url = "$JIKAN_BASE/top/anime?filter=$filter&page=$page&limit=$limit"
        return fetchJikanAnimeList(url)
    }

    private suspend fun fetchJikanAnimeList(url: String): List<Anime> {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        return runCatching {
            json.decodeFromString<JikanListResponse>(body).data.map { it.toAnimeDomain() }
        }.getOrElse {
            Timber.tag(TAG).w(it, "Parse error for $url")
            emptyList()
        }
    }

    private suspend fun fetchJikanRecommendations(url: String, limit: Int): List<Anime> {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        return runCatching {
            json.decodeFromString<JikanRecommendationResponse>(body)
                .data.take(limit)
                .mapNotNull { it.entry?.toAnimeDomain() }
        }.getOrElse {
            Timber.tag(TAG).w(it, "Parse error for recommendations $url")
            emptyList()
        }
    }

    // ─── Mapping extensions ───────────────────────────────────────────────────

    private fun ProviderSearchResult.toAnimeDomain(): Anime = Anime(
        id = id,
        title = title,
        coverImage = posterUrl,
        year = year,
        source = AnimeSource.UNKNOWN
    )

    private fun EnrichedMetadata.toDomain(id: String): Anime = Anime(
        id = id,
        title = title,
        titleEnglish = title,
        titleRomaji = titleRomaji,
        titleJapanese = titleNative,
        synopsis = synopsis,
        coverImage = coverImageUrl,
        bannerImage = bannerImageUrl,
        rating = score,
        genres = genres,
        studios = studios,
        year = year,
        totalEpisodes = episodeCount,
        isAdult = isAdult,
        anilistId = unifiedId.anilistId?.toInt(),
        malId = unifiedId.malId?.toInt(),
        status = when (status?.lowercase()) {
            "finished airing" -> AnimeStatus.FINISHED
            "currently airing" -> AnimeStatus.RELEASING
            "not yet aired" -> AnimeStatus.NOT_YET_RELEASED
            else -> AnimeStatus.UNKNOWN
        }
    )

    private fun CachedAnimeEntity.toDomain(): Anime = Anime(
        id = id,
        title = title,
        titleJapanese = titleJapanese,
        synopsis = description,
        coverImage = coverImage,
        bannerImage = bannerImage,
        year = releaseYear,
        rating = rating,
        genres = this.genres?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        totalEpisodes = totalEpisodes,
        anilistId = anilistId?.toInt(),
        malId = malId?.toInt(),
        kitsuId = kitsuId,
        status = when (this.status) {
            "RELEASING" -> AnimeStatus.RELEASING
            "FINISHED" -> AnimeStatus.FINISHED
            "NOT_YET_RELEASED" -> AnimeStatus.NOT_YET_RELEASED
            else -> AnimeStatus.UNKNOWN
        }
    )

    private fun Anime.toEntity(): CachedAnimeEntity = CachedAnimeEntity(
        id = id,
        title = title,
        titleJapanese = titleJapanese,
        description = synopsis,
        coverImage = coverImage,
        bannerImage = bannerImage,
        status = status.name,
        releaseYear = year,
        genres = genres.joinToString(","),
        rating = rating,
        totalEpisodes = totalEpisodes,
        cachedAt = System.currentTimeMillis(),
        malId = malId?.toLong(),
        anilistId = anilistId?.toLong(),
        kitsuId = kitsuId
    )

    private fun ProviderEpisode.toDomain(animeId: String): Episode = Episode(
        id = id,
        animeId = animeId,
        number = number,
        title = title,
        synopsis = description,
        thumbnail = thumbnailUrl,
        airDate = airDate,
        duration = durationMinutes
    )

    private fun com.kurostream.tv.domain.provider.StreamQuality.toDomainQuality()
        : com.kurostream.tv.domain.model.StreamQuality = when {
        pixels >= 2000 -> com.kurostream.tv.domain.model.StreamQuality.P2160
        pixels >= 1440 -> com.kurostream.tv.domain.model.StreamQuality.P1440
        pixels >= 1080 -> com.kurostream.tv.domain.model.StreamQuality.P1080
        pixels >= 720  -> com.kurostream.tv.domain.model.StreamQuality.P720
        pixels >= 480  -> com.kurostream.tv.domain.model.StreamQuality.P480
        pixels >= 360  -> com.kurostream.tv.domain.model.StreamQuality.P360
        else           -> com.kurostream.tv.domain.model.StreamQuality.AUTO
    }

    private fun com.kurostream.tv.domain.provider.StreamType.toDomainType()
        : StreamType = when (this) {
        com.kurostream.tv.domain.provider.StreamType.HLS      -> StreamType.HLS
        com.kurostream.tv.domain.provider.StreamType.DASH     -> StreamType.DASH
        com.kurostream.tv.domain.provider.StreamType.TORRENT  -> StreamType.TORRENT
        com.kurostream.tv.domain.provider.StreamType.EXTERNAL -> StreamType.DIRECT
        com.kurostream.tv.domain.provider.StreamType.DIRECT   -> StreamType.DIRECT
    }

    // ─── Local Jikan response models ──────────────────────────────────────────

    @Serializable
    private data class JikanListResponse(
        val data: List<JikanAnimeItem> = emptyList()
    )

    @Serializable
    private data class JikanAnimeItem(
        @SerialName("mal_id") val malId: Long = 0,
        val title: String = "",
        @SerialName("title_english") val titleEnglish: String? = null,
        val synopsis: String? = null,
        val score: Float? = null,
        val episodes: Int? = null,
        val status: String? = null,
        val year: Int? = null,
        val genres: List<JikanNamedItem> = emptyList(),
        val images: JikanImages? = null
    ) {
        fun toAnimeDomain(): Anime = Anime(
            id = malId.toString(),
            title = titleEnglish ?: title,
            titleJapanese = title,
            synopsis = synopsis,
            coverImage = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
            rating = score,
            totalEpisodes = episodes,
            year = year,
            genres = genres.map { it.name },
            malId = malId.toInt(),
            status = when (status?.lowercase()) {
                "finished airing"  -> AnimeStatus.FINISHED
                "currently airing" -> AnimeStatus.RELEASING
                "not yet aired"    -> AnimeStatus.NOT_YET_RELEASED
                else               -> AnimeStatus.UNKNOWN
            }
        )
    }

    @Serializable
    private data class JikanNamedItem(val name: String = "")

    @Serializable
    private data class JikanImages(val jpg: JikanJpeg? = null)

    @Serializable
    private data class JikanJpeg(
        @SerialName("image_url") val imageUrl: String? = null,
        @SerialName("large_image_url") val largeImageUrl: String? = null
    )

    @Serializable
    private data class JikanRecommendationResponse(
        val data: List<JikanRecommendationItem> = emptyList()
    )

    @Serializable
    private data class JikanRecommendationItem(
        val entry: JikanAnimeItem? = null
    )
}
