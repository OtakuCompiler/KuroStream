// KuroStream - Anime Streaming for Android TV
// Copyright (C) 2026 KuroStream Contributors
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.data.anilist

import com.kurostream.data.anilist.model.AniListMedia
import com.kurostream.data.anilist.model.AniListSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRepository @Inject constructor(
    private val apolloClient: AniListApolloClient,
) {
    fun searchAnime(query: String, page: Int = 1, perPage: Int = 20): Flow<Result<AniListSearchResult>> = flow {
        try {
            val response = apolloClient.client.query(
                SearchAnimeQuery(query = query, page = page, perPage = perPage)
            ).execute()
            
            if (response.hasErrors()) {
                emit(Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error")))
                return@flow
            }
            
            val data = response.data
            if (data == null) {
                emit(Result.failure(Exception("No data returned")))
                return@flow
            }
            
            val result = AniListSearchResult(
                page = data.page?.pageInfo?.currentPage ?: 1,
                hasNextPage = data.page?.pageInfo?.hasNextPage ?: false,
                media = data.page?.media?.filterNotNull()?.map { media ->
                    AniListMedia(
                        id = media.id,
                        idMal = media.idMal,
                        title = media.title?.english 
                            ?: media.title?.romaji 
                            ?: media.title?.native 
                            ?: "Unknown",
                        coverImage = media.coverImage?.large ?: "",
                        bannerImage = media.bannerImage ?: "",
                        description = media.description ?: "",
                        episodes = media.episodes,
                        duration = media.duration,
                        status = media.status?.rawValue ?: "",
                        season = media.season?.rawValue ?: "",
                        seasonYear = media.seasonYear,
                        averageScore = media.averageScore,
                        genres = media.genres?.filterNotNull() ?: emptyList(),
                        studios = media.studios?.nodes?.filterNotNull()?.map { it.name } ?: emptyList(),
                        nextAiringEpisode = media.nextAiringEpisode?.let {
                            AniListMedia.NextAiringEpisode(
                                episode = it.episode,
                                timeUntilAiring = it.timeUntilAiring
                            )
                        }
                    )
                } ?: emptyList()
            )
            emit(Result.success(result))
        } catch (e: Exception) {
            Timber.e(e, "AniList search failed")
            emit(Result.failure(e))
        }
    }

    fun getAnimeDetails(id: Int): Flow<Result<AniListMedia>> = flow {
        try {
            val response = apolloClient.client.query(
                AnimeDetailsQuery(id = id)
            ).execute()
            
            if (response.hasErrors()) {
                emit(Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error")))
                return@flow
            }
            
            val media = response.data?.media
            if (media == null) {
                emit(Result.failure(Exception("Media not found")))
                return@flow
            }
            
            emit(Result.success(mapMedia(media)))
        } catch (e: Exception) {
            Timber.e(e, "AniList details fetch failed")
            emit(Result.failure(e))
        }
    }

    private fun mapMedia(media: AnimeDetailsQuery.Media): AniListMedia {
        return AniListMedia(
            id = media.id,
            idMal = media.idMal,
            title = media.title?.english 
                ?: media.title?.romaji 
                ?: media.title?.native 
                ?: "Unknown",
            coverImage = media.coverImage?.large ?: "",
            bannerImage = media.bannerImage ?: "",
            description = media.description ?: "",
            episodes = media.episodes,
            duration = media.duration,
            status = media.status?.rawValue ?: "",
            season = media.season?.rawValue ?: "",
            seasonYear = media.seasonYear,
            averageScore = media.averageScore,
            genres = media.genres?.filterNotNull() ?: emptyList(),
            studios = media.studios?.nodes?.filterNotNull()?.map { it.name } ?: emptyList(),
            nextAiringEpisode = media.nextAiringEpisode?.let {
                AniListMedia.NextAiringEpisode(
                    episode = it.episode,
                    timeUntilAiring = it.timeUntilAiring
                )
            }
        )
    }
}
