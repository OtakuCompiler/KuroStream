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

package com.kurostream.data.skip

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkipRepository @Inject constructor(
    private val aniSkipClient: AniSkipClient,
    private val introDbClient: IntroDbClient,
    private val mlIntroDetector: MlIntroDetector,
) {
    fun getSkipTimes(
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int,
        episodeLength: Double = 24.0,
    ): Flow<Result<Map<SkipType, SkipInterval>>> = flow {
        val result = try {
            coroutineScope {
                val aniSkipDeferred = malId?.let {
                    async { aniSkipClient.getSkipTimes(it, episodeNumber, episodeLength) }
                }
                val introDbDeferred = anilistId?.let {
                    async { introDbClient.getSkipTimes(it, episodeNumber) }
                }
                val mlDeferred = async { mlIntroDetector.detectFromMetadata(malId, anilistId, episodeNumber) }
                
                val aniSkipResult = aniSkipDeferred?.await()
                if (aniSkipResult?.isSuccess == true && aniSkipResult.getOrThrow().isNotEmpty()) {
                    Timber.d("Using AniSkip data")
                    return@coroutineScope aniSkipResult
                }
                
                val introDbResult = introDbDeferred?.await()
                if (introDbResult?.isSuccess == true && introDbResult.getOrThrow().isNotEmpty()) {
                    Timber.d("Using IntroDB data")
                    return@coroutineScope introDbResult
                }
                
                val mlResult = mlDeferred.await()
                if (mlResult.isSuccess && mlResult.getOrThrow().isNotEmpty()) {
                    Timber.d("Using ML detection data")
                    return@coroutineScope mlResult
                }
                
                val webResult = webScrapeSkipTimes(malId, anilistId, episodeNumber)
                if (webResult.isSuccess && webResult.getOrThrow().isNotEmpty()) {
                    Timber.d("Using web-scraped data")
                    return@coroutineScope webResult
                }
                
                Result.failure(Exception("No skip data available from any source"))
            }
        } catch (e: Exception) {
            Timber.e(e, "All skip sources failed")
            Result.failure(e)
        }
        emit(result)
    }
    
    private suspend fun webScrapeSkipTimes(
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int,
    ): Result<Map<SkipType, SkipInterval>> {
        return Result.failure(Exception("Web scrape not implemented"))
    }
}
