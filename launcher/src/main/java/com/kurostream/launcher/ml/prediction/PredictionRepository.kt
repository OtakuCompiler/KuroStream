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

package com.kurostream.launcher.ml.prediction

import com.kurostream.launcher.data.local.WatchHistoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(
    private val predictor: NextEpisodePredictor,
    private val watchHistoryDao: WatchHistoryDao
) {

    /**
     * Get prediction for next episode with caching
     */
    fun getNextEpisodePrediction(
        seriesId: String,
        currentEpisode: Int,
        completionPercentage: Float
    ): Flow<PredictionResult> = flow {
        val result = predictor.predictNextEpisodeWatch(
            seriesId = seriesId,
            currentEpisodeNumber = currentEpisode,
            completionPercentage = completionPercentage
        )
        emit(result)
    }

    /**
     * Batch predict for multiple series
     */
    suspend fun getBatchPredictions(
        requests: List<PredictionRequest>
    ): List<Pair<PredictionRequest, PredictionResult>> {
        return requests.map { request ->
            val result = predictor.predictNextEpisodeWatch(
                seriesId = request.seriesId,
                currentEpisodeNumber = request.currentEpisode,
                completionPercentage = request.completionPercentage
            )
            request to result
        }
    }

    /**
     * Get series ranked by likelihood of next watch
     */
    suspend fun getRankedSeries(seriesIds: List<String>): List<SeriesPrediction> {
        return seriesIds.mapNotNull { seriesId ->
            val history = watchHistoryDao.getHistoryForSeries(seriesId)
            val lastWatch = history.maxByOrNull { it.watchedAt } ?: return@mapNotNull null

            val result = predictor.predictNextEpisodeWatch(
                seriesId = seriesId,
                currentEpisodeNumber = lastWatch.episodeNumber,
                completionPercentage = lastWatch.completionPercentage
            )

            SeriesPrediction(
                seriesId = seriesId,
                confidence = result.confidence,
                estimatedWatchTime = result.estimatedTimeToWatch,
                reason = result.reason
            )
        }.sortedByDescending { it.confidence }
    }
}

data class PredictionRequest(
    val seriesId: String,
    val currentEpisode: Int,
    val completionPercentage: Float
)

data class SeriesPrediction(
    val seriesId: String,
    val confidence: Float,
    val estimatedWatchTime: Long,
    val reason: String
)
