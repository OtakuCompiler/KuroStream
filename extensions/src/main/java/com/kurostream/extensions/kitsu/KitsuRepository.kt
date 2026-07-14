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

package com.kurostream.extensions.kitsu

import com.kurostream.extensions.domain.model.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KitsuRepository @Inject constructor(
    private val api: KitsuApi
) {

    suspend fun getTrendingAnime(limit: Int = 20): Result<List<CatalogItem>> = try {
        val response = api.getTrendingAnime(limit)
        if (response.isSuccessful) {
            val items = response.body()?.data?.map { it.toCatalogItem() } ?: emptyList()
            Result.success(items)
        } else {
            Result.failure(Exception("Trending fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSeasonalAnime(season: String, year: Int, limit: Int = 20): Result<List<CatalogItem>> = try {
        val response = api.getSeasonalAnime(season, year, limit)
        if (response.isSuccessful) {
            val items = response.body()?.data?.map { it.toCatalogItem() } ?: emptyList()
            Result.success(items)
        } else {
            Result.failure(Exception("Seasonal fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPopularAnime(limit: Int = 20): Result<List<CatalogItem>> = try {
        val response = api.getPopularAnime(limit)
        if (response.isSuccessful) {
            val items = response.body()?.data?.map { it.toCatalogItem() } ?: emptyList()
            Result.success(items)
        } else {
            Result.failure(Exception("Popular fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchAnime(query: String, limit: Int = 20): Result<List<CatalogItem>> = try {
        val response = api.getAnimeList(limit = limit, search = query)
        if (response.isSuccessful) {
            val items = response.body()?.data?.map { it.toCatalogItem() } ?: emptyList()
            Result.success(items)
        } else {
            Result.failure(Exception("Search failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAnimeDetails(id: String): Result<MediaDetail> = try {
        val response = api.getAnime(id)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.data.toMediaDetail(body.included))
        } else {
            Result.failure(Exception("Details fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAnimeCharacters(animeId: String): Result<List<CharacterInfo>> = try {
        val response = api.getAnimeCharacters(animeId, limit = 20)
        if (response.isSuccessful) {
            val characters = response.body()?.data?.mapNotNull { charData ->
                charData.attributes?.let { attr ->
                    CharacterInfo(
                        id = charData.id,
                        name = attr.name ?: attr.canonicalName ?: "Unknown",
                        imageUrl = attr.image?.medium ?: attr.image?.small,
                        role = null
                    )
                }
            } ?: emptyList()
            Result.success(characters)
        } else {
            Result.failure(Exception("Characters fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAnimeStaff(animeId: String): Result<List<StaffInfo>> = try {
        val response = api.getAnimeStaff(animeId, limit = 20)
        if (response.isSuccessful) {
            val staff = response.body()?.data?.mapNotNull { staffData ->
                staffData.attributes?.let { attr ->
                    StaffInfo(
                        id = staffData.id,
                        name = attr.name ?: "Unknown",
                        imageUrl = attr.image?.medium ?: attr.image?.small,
                        role = attr.role
                    )
                }
            } ?: emptyList()
            Result.success(staff)
        } else {
            Result.failure(Exception("Staff fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getCurrentSeason(): Pair<String, Int> {
        val now = LocalDate.now()
        val year = now.year
        val season = when (now.monthValue) {
            in 1..3 -> "winter"
            in 4..6 -> "spring"
            in 7..9 -> "summer"
            else -> "fall"
        }
        return season to year
    }
}

private fun KitsuAnimeData.toCatalogItem(): CatalogItem = CatalogItem(
    id = "kitsu:$id",
    title = attributes.canonicalTitle ?: attributes.titles?.en ?: attributes.titles?.enJp ?: "Unknown",
    posterUrl = attributes.posterImage?.medium ?: attributes.posterImage?.small,
    backdropUrl = attributes.coverImage?.large ?: attributes.coverImage?.medium,
    type = "anime",
    year = attributes.startDate?.take(4),
    rating = attributes.averageRating?.toFloatOrNull()?.div(10f),
    genres = emptyList(),
    source = "kitsu"
)

private fun KitsuAnimeData.toMediaDetail(included: List<KitsuIncluded>?): MediaDetail {
    val genres = included?.filter { it.type == "genres" }?.mapNotNull { it.attributes?.name } ?: emptyList()
    val characters = included?.filter { it.type == "characters" }?.mapNotNull {
        it.attributes?.let { attr ->
            CharacterInfo(
                id = it.id,
                name = attr.name ?: attr.canonicalName ?: "Unknown",
                imageUrl = attr.image?.medium ?: attr.image?.small,
                role = null
            )
        }
    } ?: emptyList()
    val staff = included?.filter { it.type == "people" }?.mapNotNull {
        it.attributes?.let { attr ->
            StaffInfo(
                id = it.id,
                name = attr.name ?: "Unknown",
                imageUrl = attr.image?.medium ?: attr.image?.small,
                role = attr.role
            )
        }
    } ?: emptyList()

    return MediaDetail(
        id = "kitsu:$id",
        title = attributes.canonicalTitle ?: attributes.titles?.en ?: attributes.titles?.enJp ?: "Unknown",
        description = attributes.synopsis,
        posterUrl = attributes.posterImage?.large ?: attributes.posterImage?.medium,
        backdropUrl = attributes.coverImage?.original ?: attributes.coverImage?.large,
        type = "anime",
        rating = attributes.averageRating?.toFloatOrNull()?.div(10f),
        genres = genres,
        cast = emptyList(),
        director = emptyList(),
        year = attributes.startDate?.take(4),
        runtime = attributes.episodeLength?.toString(),
        episodeCount = attributes.episodeCount,
        status = attributes.status,
        ageRating = attributes.ageRating,
        characters = characters,
        staff = staff,
        episodes = emptyList()
    )
}
