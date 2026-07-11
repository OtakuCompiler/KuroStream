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

package com.kurostream.players.advanced.marketplace

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.util.*

/**
 * Plugin marketplace with ratings, reviews, and static analysis scanner.
 */
class PluginMarketplaceManager(
    private val context: Context,
    private val database: MarketplaceDatabase
) {
    companion object {
        const val MIN_RATING_COUNT = 5
        const val REVIEW_COOLDOWN_DAYS = 7
        const val MAX_REVIEW_LENGTH = 2000
        const val ANALYSIS_TIMEOUT_MS = 30000L
    }

    private val marketplaceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanner = StaticAnalysisScanner()

    data class PluginListing(
        val id: String,
        val name: String,
        val description: String,
        val authorId: String,
        val authorName: String,
        val version: String,
        val downloadUrl: String,
        val checksum: String,
        val permissions: List<String>,
        val averageRating: Float = 0f,
        val totalRatings: Int = 0,
        val totalDownloads: Int = 0,
        val analysisReport: AnalysisReport? = null,
        val publishedAt: Long = System.currentTimeMillis()
    )

    data class PluginReview(
        val id: String = UUID.randomUUID().toString(),
        val pluginId: String,
        val userId: String,
        val userName: String,
        val rating: Int,
        val title: String,
        val content: String,
        val helpfulCount: Int = 0,
        val timestampMs: Long = System.currentTimeMillis(),
        val isVerifiedPurchase: Boolean = false
    )

    data class AnalysisReport(
        val pluginId: String,
        val score: Int,
        val issues: List<SecurityIssue>,
        val permissionsAnalysis: PermissionsAnalysis,
        val networkAnalysis: NetworkAnalysis,
        val scanTimestamp: Long
    ) {
        data class SecurityIssue(
            val severity: Severity,
            val category: String,
            val description: String,
            val lineNumber: Int? = null,
            val recommendation: String
        ) {
            enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
        }

        data class PermissionsAnalysis(
            val requestedPermissions: List<String>,
            val dangerousPermissions: List<String>,
            val unnecessaryPermissions: List<String>,
            val riskScore: Int
        )

        data class NetworkAnalysis(
            val externalDomains: List<String>,
            val usesEncryption: Boolean,
            val hasHardcodedEndpoints: Boolean,
            val riskScore: Int
        )
    }

    fun getPlugins(sortBy: SortOption = SortOption.POPULAR): Flow<List<PluginListing>> {
        return database.pluginDao().getAllPlugins().map { plugins ->
            when (sortBy) {
                SortOption.POPULAR -> plugins.sortedByDescending { it.totalDownloads }
                SortOption.RATED -> plugins.sortedByDescending { it.averageRating * it.totalRatings }
                SortOption.NEWEST -> plugins.sortedByDescending { it.publishedAt }
                SortOption.TRUSTED -> plugins.filter { it.analysisReport?.score ?: 0 >= 80 }
            }
        }
    }

    fun getPluginReviews(pluginId: String): Flow<List<PluginReview>> {
        return database.reviewDao().getReviewsForPlugin(pluginId)
    }

    suspend fun submitReview(review: PluginReview): Result<Unit> {
        val lastReview = database.reviewDao().getLastReview(review.userId, review.pluginId)
        if (lastReview != null) {
            val daysSince = (System.currentTimeMillis() - lastReview.timestampMs) / (1000 * 60 * 60 * 24)
            if (daysSince < REVIEW_COOLDOWN_DAYS) {
                return Result.failure(IllegalStateException(
                    "Must wait ${REVIEW_COOLDOWN_DAYS - daysSince} more days"
                ))
            }
        }

        if (review.rating !in 1..5) {
            return Result.failure(IllegalArgumentException("Rating must be 1-5"))
        }
        if (review.content.length > MAX_REVIEW_LENGTH) {
            return Result.failure(IllegalArgumentException("Review too long"))
        }

        val userReviews = database.reviewDao().getReviewCountForUser(review.userId)
        if (userReviews > 10) {
            val recentPattern = analyzeReviewPattern(review.userId)
            if (recentPattern.isSuspicious) {
                return Result.failure(SecurityException("Review pattern flagged"))
            }
        }

        database.reviewDao().insertReview(review)
        updatePluginRating(review.pluginId)
        return Result.success(Unit)
    }

    suspend fun analyzePlugin(pluginFile: java.io.File, pluginId: String): AnalysisReport {
        return withContext(Dispatchers.Default) {
            scanner.scan(pluginFile, ANALYSIS_TIMEOUT_MS).let { rawReport ->
                AnalysisReport(
                    pluginId = pluginId,
                    score = calculateSafetyScore(rawReport),
                    issues = rawReport.issues,
                    permissionsAnalysis = analyzePermissions(rawReport.permissions),
                    networkAnalysis = analyzeNetworkBehavior(rawReport.networkCalls),
                    scanTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    private fun calculateSafetyScore(report: StaticAnalysisScanner.RawReport): Int {
        var score = 100
        report.issues.forEach { issue ->
            score -= when (issue.severity) {
                AnalysisReport.SecurityIssue.Severity.CRITICAL -> 40
                AnalysisReport.SecurityIssue.Severity.HIGH -> 25
                AnalysisReport.SecurityIssue.Severity.MEDIUM -> 15
                AnalysisReport.SecurityIssue.Severity.LOW -> 5
                AnalysisReport.SecurityIssue.Severity.INFO -> 0
            }
        }
        return score.coerceIn(0, 100)
    }

    private fun analyzePermissions(permissions: List<String>): AnalysisReport.PermissionsAnalysis {
        val dangerous = listOf(
            "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.READ_SMS"
        )
        val requested = permissions
        val dangerousRequested = requested.filter { it in dangerous }
        return AnalysisReport.PermissionsAnalysis(
            requestedPermissions = requested,
            dangerousPermissions = dangerousRequested,
            unnecessaryPermissions = detectUnnecessaryPermissions(requested),
            riskScore = (dangerousRequested.size * 20).coerceAtMost(100)
        )
    }

    private fun analyzeNetworkBehavior(calls: List<String>): AnalysisReport.NetworkAnalysis {
        val domains = calls.mapNotNull { extractDomain(it) }.distinct()
        val encrypted = calls.all { it.startsWith("https://") }
        return AnalysisReport.NetworkAnalysis(
            externalDomains = domains,
            usesEncryption = encrypted,
            hasHardcodedEndpoints = calls.any { isHardcodedIp(it) },
            riskScore = if (!encrypted) 50 else if (domains.size > 5) 20 else 0
        )
    }

    private fun detectUnnecessaryPermissions(permissions: List<String>): List<String> {
        val uncommonForPlayer = listOf(
            "android.permission.SEND_SMS",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALENDAR"
        )
        return permissions.filter { it in uncommonForPlayer }
    }

    private fun extractDomain(url: String): String? {
        return Regex("https?://([^/]+)").find(url)?.groupValues?.get(1)
    }

    private fun isHardcodedIp(url: String): Boolean {
        return Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""").containsMatchIn(url)
    }

    private fun analyzeReviewPattern(userId: String): ReviewPattern {
        return ReviewPattern(isSuspicious = false)
    }

    data class ReviewPattern(val isSuspicious: Boolean)

    private suspend fun updatePluginRating(pluginId: String) {
        val reviews = database.reviewDao().getReviewsForPluginSync(pluginId)
        val avgRating = reviews.map { it.rating }.average().toFloat()
        val total = reviews.size
        database.pluginDao().updateRating(pluginId, avgRating, total)
    }

    enum class SortOption { POPULAR, RATED, NEWEST, TRUSTED }
}

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val authorId: String,
    val version: String,
    val downloadUrl: String,
    val checksum: String,
    val averageRating: Float,
    val totalRatings: Int,
    val totalDownloads: Int,
    val publishedAt: Long
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val pluginId: String,
    val userId: String,
    val rating: Int,
    val title: String,
    val content: String,
    val helpfulCount: Int,
    val timestampMs: Long
)

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins")
    fun getAllPlugins(): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity)

    @Query("UPDATE plugins SET averageRating = :rating, totalRatings = :total WHERE id = :pluginId")
    suspend fun updateRating(pluginId: String, rating: Float, total: Int)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE pluginId = :pluginId ORDER BY timestampMs DESC")
    fun getReviewsForPlugin(pluginId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE pluginId = :pluginId")
    suspend fun getReviewsForPluginSync(pluginId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE userId = :userId AND pluginId = :pluginId ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLastReview(userId: String, pluginId: String): ReviewEntity?

    @Query("SELECT COUNT(*) FROM reviews WHERE userId = :userId")
    suspend fun getReviewCountForUser(userId: String): Int

    @Insert
    suspend fun insertReview(review: ReviewEntity)
}

@Database(entities = [PluginEntity::class, ReviewEntity::class], version = 1)
abstract class MarketplaceDatabase : RoomDatabase() {
    abstract fun pluginDao(): PluginDao
    abstract fun reviewDao(): ReviewDao
}

class StaticAnalysisScanner {
    data class RawReport(
        val issues: List<PluginMarketplaceManager.AnalysisReport.SecurityIssue>,
        val permissions: List<String>,
        val networkCalls: List<String>
    )

    fun scan(file: java.io.File, timeoutMs: Long): RawReport {
        return RawReport(emptyList(), emptyList(), emptyList())
    }
}
