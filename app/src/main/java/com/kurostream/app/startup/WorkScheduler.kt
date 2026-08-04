package com.kurostream.app.startup

import android.content.Context
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleAll(context: Context) {
    }
}
