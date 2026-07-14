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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class PredictionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val predictionRepository: PredictionRepository,
    private val watchHistoryDao: com.kurostream.launcher.data.local.WatchHistoryDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "prediction_worker"
        const val KEY_SERIES_ID = "series_id"
        const val KEY_EPISODE = "episode_number"
        const val KEY_COMPLETION = "completion_percentage"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            val seriesId = inputData.getString(KEY_SERIES_ID) ?: return@withContext Result.failure()
            val episode = inputData.getInt(KEY_EPISODE, 1)
            val completion = inputData.getFloat(KEY_COMPLETION, 0f)

            val result = predictionRepository.getNextEpisodePrediction(
                seriesId = seriesId,
                currentEpisode = episode,
                completionPercentage = completion
            ).first()

            // Store prediction result for cache manager to use
            val outputData = androidx.work.Data.Builder()
                .putBoolean("will_watch", result.willWatch)
                .putFloat("confidence", result.confidence)
                .putLong("estimated_time", result.estimatedTimeToWatch)
                .putString("reason", result.reason)
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
