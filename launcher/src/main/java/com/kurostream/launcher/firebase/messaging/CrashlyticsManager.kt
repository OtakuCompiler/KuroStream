package com.kurostream.launcher.firebase.messaging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashlyticsManager @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    fun log(message: String) {
        crashlytics.log(message)
    }

    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
