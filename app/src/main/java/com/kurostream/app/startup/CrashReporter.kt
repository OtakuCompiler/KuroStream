package com.kurostream.app.startup

import android.content.Context
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor() {
    fun initialize(context: Context) {
        Timber.plant(CrashReportingTree())
    }

    class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        }
    }
}
