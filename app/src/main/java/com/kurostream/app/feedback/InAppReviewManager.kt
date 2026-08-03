package com.kurostream.app.feedback

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.ReviewInfo
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class InAppReviewManager {
    suspend fun requestReview(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            val reviewInfo = request.await()
            manager.launchReviewFlow(activity, reviewInfo)
        } catch (e: Exception) {
            Timber.e(e, "In-app review failed")
        }
    }
}
