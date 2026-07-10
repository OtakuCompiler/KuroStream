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
import com.kurostream.launcher.data.local.WatchHistoryDao
import com.kurostream.launcher.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextEpisodePredictor @Inject constructor(
    private val context: Context,
    private val modelLoader: TFLiteModelLoader,
    private val featureExtractor: FeatureExtractor,
    private val watchHistoryDao: WatchHistoryDao
) {
    private var interpreter: Interpreter? = null
    private val modelLock = Any()

    companion object {
        private const val MODEL_INPUT_SIZE = 64 // Feature vector size
        private const val MODEL_OUTPUT_SIZE = 1 // Probability score
        private const val CONFIDENCE_THRESHOLD = 0.75f
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            synchronized(modelLock) {
                if (interpreter == null) {
                    interpreter = modelLoader.loadModel("next_episode_model.tflite")
                }
            }
        } catch (e: Exception) {
            // Model loading failed, use heuristic fallback
        }
    }

    /**
     * Predict whether user will watch next episode
     * @return Prediction result with confidence score
     */
    suspend fun predictNextEpisodeWatch(
        seriesId: String,
        currentEpisodeNumber: Int,
        completionPercentage: Float
    ): PredictionResult = withContext(Dispatchers.Default) {
        try {
            val features = featureExtractor.extractFeatures(
                seriesId = seriesId,
                currentEpisode = currentEpisodeNumber,
                completionPercentage = completionPercentage,
                watchHistoryDao = watchHistoryDao
            )

            val prediction = runInference(features)
            val confidence = prediction.first()

            PredictionResult(
                willWatch = confidence > CONFIDENCE_THRESHOLD,
                confidence = confidence,
                estimatedTimeToWatch = estimateTimeToWatch(features),
                reason = generateReason(features, confidence)
            )
        } catch (e: Exception) {
            // Fallback to heuristic prediction
            heuristicPrediction(seriesId, currentEpisodeNumber, completionPercentage)
        }
    }

    /**
     * Run TFLite inference
     */
    private fun runInference(features: FloatArray): FloatArray {
        val interpreter = this.interpreter
            ?: return floatArrayOf(0.5f) // Default if model not loaded

        val inputBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE * 4)
            .order(ByteOrder.nativeOrder())
        features.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val outputBuffer = ByteBuffer.allocateDirect(MODEL_OUTPUT_SIZE * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        return FloatArray(MODEL_OUTPUT_SIZE) { outputBuffer.getFloat() }
    }

    /**
     * Heuristic fallback when model is unavailable
     */
    private suspend fun heuristicPrediction(
        seriesId: String,
        currentEpisode: Int,
        completionPercentage: Float
    ): PredictionResult {
        val history = watchHistoryDao.getHistoryForSeries(seriesId)
        val avgCompletion = history.map { it.completionPercentage }.average().toFloat()
        val bingeScore = calculateBingeScore(history)

        val confidence = when {
            completionPercentage > 0.9f && bingeScore > 0.7f -> 0.9f
            completionPercentage > 0.8f && bingeScore > 0.5f -> 0.75f
            completionPercentage > 0.7f -> 0.6f
            else -> 0.3f
        }

        return PredictionResult(
            willWatch = confidence > CONFIDENCE_THRESHOLD,
            confidence = confidence,
            estimatedTimeToWatch = estimateTimeToWatchHeuristic(history),
            reason = "Based on your ${history.size} previous watches"
        )
    }

    private fun calculateBingeScore(history: List<WatchHistoryEntity>): Float {
        if (history.size < 2) return 0.5f
        val sorted = history.sortedBy { it.watchedAt }
        var consecutiveCount = 1
        var maxConsecutive = 1

        for (i in 1 until sorted.size) {
            val diff = sorted[i].watchedAt - sorted[i - 1].watchedAt
            if (diff < 3600000L) { // Less than 1 hour between episodes
                consecutiveCount++
                maxConsecutive = maxOf(maxConsecutive, consecutiveCount)
            } else {
                consecutiveCount = 1
            }
        }

        return (maxConsecutive.toFloat() / history.size).coerceIn(0f, 1f)
    }

    private fun estimateTimeToWatch(features: FloatArray): Long {
        // Estimate based on features (simplified)
        return 300000L // 5 minutes default
    }

    private fun estimateTimeToWatchHeuristic(history: List<WatchHistoryEntity>): Long {
        if (history.isEmpty()) return 600000L // 10 minutes
        val avgGap = history.zipWithNext { a, b -> b.watchedAt - a.watchedAt }
            .filter { it > 0 }
            .average()
        return if (avgGap.isFinite()) avgGap.toLong() else 300000L
    }

    private fun generateReason(features: FloatArray, confidence: Float): String {
        return when {
            confidence > 0.9f -> "You're on a binge streak!"
            confidence > 0.75f -> "You usually continue this series"
            confidence > 0.5f -> "Might continue watching"
            else -> "Uncertain if you'll continue"
        }
    }

    fun close() {
        synchronized(modelLock) {
            interpreter?.close()
            interpreter = null
        }
    }
}

data class PredictionResult(
    val willWatch: Boolean,
    val confidence: Float,
    val estimatedTimeToWatch: Long, // milliseconds
    val reason: String
)
