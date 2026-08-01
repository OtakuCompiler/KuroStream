package com.kurostream.app.feedback

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

class InAppReviewManager {
    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
