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
import com.kurostream.launcher.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class FeatureExtractor {

    companion object {
        private const val FEATURE_VECTOR_SIZE = 64
        private const val MAX_HISTORY_LENGTH = 20
        private const val HOUR_MS = 3600000L
        private const val DAY_MS = 86400000L
    }

    /**
     * Extract feature vector for prediction model
     */
    suspend fun extractFeatures(
        seriesId: String,
        currentEpisode: Int,
        completionPercentage: Float,
        watchHistoryDao: WatchHistoryDao
    ): FloatArray = withContext(Dispatchers.Default) {
        val history = watchHistoryDao.getHistoryForSeries(seriesId)
        val allHistory = watchHistoryDao.getAllHistory()

        val features = FloatArray(FEATURE_VECTOR_SIZE) { 0f }
        var idx = 0

        // 1. Completion percentage (normalized 0-1)
        features[idx++] = completionPercentage.coerceIn(0f, 1f)

        // 2. Current episode number (normalized)
        features[idx++] = min(currentEpisode / 50f, 1f)

        // 3. Series watch history statistics
        val seriesHistory = history.filter { it.seriesId == seriesId }
        features[idx++] = min(seriesHistory.size / MAX_HISTORY_LENGTH.toFloat(), 1f)
        features[idx++] = seriesHistory.map { it.completionPercentage }.average().toFloat().coerceIn(0f, 1f)

        // 4. Binge behavior patterns
        val bingeFeatures = extractBingeFeatures(seriesHistory)
        features[idx++] = bingeFeatures.avgTimeBetweenEpisodes
        features[idx++] = bingeFeatures.maxConsecutiveEpisodes / 10f
        features[idx++] = bingeFeatures.bingeScore

        // 5. Temporal features
        val temporalFeatures = extractTemporalFeatures(seriesHistory)
        features[idx++] = temporalFeatures.hourOfDay / 24f
        features[idx++] = temporalFeatures.dayOfWeek / 7f
        features[idx++] = temporalFeatures.isWeekend
        features[idx++] = temporalFeatures.isEvening

        // 6. User engagement metrics
        val engagementFeatures = extractEngagementFeatures(allHistory)
        features[idx++] = engagementFeatures.totalWatchedHours / 100f
        features[idx++] = engagementFeatures.avgSessionDuration / 120f // normalize to 2 hours
        features[idx++] = engagementFeatures.completionRate
        features[idx++] = engagementFeatures.dropOffRate

        // 7. Series-specific patterns
        val seriesPatterns = extractSeriesPatterns(seriesHistory, currentEpisode)
        features[idx++] = seriesPatterns.avgCompletionForEpisode
        features[idx++] = seriesPatterns.watchedInLast24h
        features[idx++] = seriesPatterns.watchedInLastWeek
        features[idx++] = seriesPatterns.trendDirection // -1 to 1

        // 8. Content features (placeholder for genre, rating, etc.)
        features[idx++] = 0.5f // genre affinity placeholder
        features[idx++] = 0.5f // rating placeholder
        features[idx++] = 0.5f // popularity placeholder

        // Fill remaining with zeros
        while (idx < FEATURE_VECTOR_SIZE) {
            features[idx++] = 0f
        }

        features
    }

    private fun extractBingeFeatures(history: List<WatchHistoryEntity>): BingeFeatures {
        if (history.size < 2) return BingeFeatures(0.5f, 0f, 0.5f)

        val sorted = history.sortedBy { it.watchedAt }
        val gaps = sorted.zipWithNext { a, b -> b.watchedAt - a.watchedAt }
            .filter { it > 0 && it < DAY_MS }

        val avgGap = if (gaps.isNotEmpty()) gaps.average().toFloat() / HOUR_MS else 24f
        val avgTimeBetween = 1f - (avgGap / 24f).coerceIn(0f, 1f)

        var consecutiveCount = 1
        var maxConsecutive = 1
        for (gap in gaps) {
            if (gap < HOUR_MS * 2) {
                consecutiveCount++
                maxConsecutive = max(maxConsecutive, consecutiveCount)
            } else {
                consecutiveCount = 1
            }
        }

        val bingeScore = (maxConsecutive.toFloat() / history.size).coerceIn(0f, 1f)

        return BingeFeatures(avgTimeBetween, maxConsecutive.toFloat(), bingeScore)
    }

    private fun extractTemporalFeatures(history: List<WatchHistoryEntity>): TemporalFeatures {
        if (history.isEmpty()) {
            val now = java.util.Calendar.getInstance()
            return TemporalFeatures(
                hourOfDay = now.get(java.util.Calendar.HOUR_OF_DAY).toFloat(),
                dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK).toFloat(),
                isWeekend = if (now.get(java.util.Calendar.DAY_OF_WEEK) in listOf(1, 7)) 1f else 0f,
                isEvening = if (now.get(java.util.Calendar.HOUR_OF_DAY) in 18..23) 1f else 0f
            )
        }

        val lastWatch = java.util.Calendar.getInstance().apply {
            timeInMillis = history.maxOf { it.watchedAt }
        }

        return TemporalFeatures(
            hourOfDay = lastWatch.get(java.util.Calendar.HOUR_OF_DAY).toFloat(),
            dayOfWeek = lastWatch.get(java.util.Calendar.DAY_OF_WEEK).toFloat(),
            isWeekend = if (lastWatch.get(java.util.Calendar.DAY_OF_WEEK) in listOf(1, 7)) 1f else 0f,
            isEvening = if (lastWatch.get(java.util.Calendar.HOUR_OF_DAY) in 18..23) 1f else 0f
        )
    }

    private fun extractEngagementFeatures(allHistory: List<WatchHistoryEntity>): EngagementFeatures {
        if (allHistory.isEmpty()) return EngagementFeatures(0f, 0f, 0f, 0f)

        val totalMs = allHistory.sumOf { it.durationWatchedMs }
        val totalHours = totalMs / HOUR_MS.toFloat()
        val avgSession = allHistory.map { it.durationWatchedMs }.average().toFloat() / 60000f // minutes
        val completions = allHistory.count { it.completionPercentage > 0.9f }
        val completionRate = completions.toFloat() / allHistory.size
        val dropOffs = allHistory.count { it.completionPercentage < 0.3f }
        val dropOffRate = dropOffs.toFloat() / allHistory.size

        return EngagementFeatures(totalHours, avgSession, completionRate, dropOffRate)
    }

    private fun extractSeriesPatterns(history: List<WatchHistoryEntity>, currentEpisode: Int): SeriesPatterns {
        val now = System.currentTimeMillis()
        val last24h = history.count { now - it.watchedAt < DAY_MS }
        val lastWeek = history.count { now - it.watchedAt < DAY_MS * 7 }

        val episodeCompletions = history
            .filter { it.episodeNumber == currentEpisode }
            .map { it.completionPercentage }
        val avgCompletion = if (episodeCompletions.isNotEmpty()) {
            episodeCompletions.average().toFloat()
        } else 0.5f

        // Trend: increasing or decreasing watch frequency
        val recent = history.filter { now - it.watchedAt < DAY_MS * 7 }.size
        val older = history.filter { now - it.watchedAt in (DAY_MS * 7)..(DAY_MS * 30) }.size
        val trend = when {
            recent > older * 2 -> 1f
            recent > older -> 0.5f
            recent < older / 2 -> -1f
            else -> 0f
        }

        return SeriesPatterns(avgCompletion, last24h.toFloat(), lastWeek.toFloat(), trend)
    }

    data class BingeFeatures(
        val avgTimeBetweenEpisodes: Float,
        val maxConsecutiveEpisodes: Float,
        val bingeScore: Float
    )

    data class TemporalFeatures(
        val hourOfDay: Float,
        val dayOfWeek: Float,
        val isWeekend: Float,
        val isEvening: Float
    )

    data class EngagementFeatures(
        val totalWatchedHours: Float,
        val avgSessionDuration: Float,
        val completionRate: Float,
        val dropOffRate: Float
    )

    data class SeriesPatterns(
        val avgCompletionForEpisode: Float,
        val watchedInLast24h: Float,
        val watchedInLastWeek: Float,
        val trendDirection: Float
    )
}
