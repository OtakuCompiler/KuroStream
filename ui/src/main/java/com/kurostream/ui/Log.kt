package com.kurostream.ui
import timber.log.Timber
object Log {
    fun d(tag: String, msg: String, t: Throwable? = null) = Timber.tag(tag).d(t, msg)
    fun i(tag: String, msg: String, t: Throwable? = null) = Timber.tag(tag).i(t, msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = Timber.tag(tag).w(t, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = Timber.tag(tag).e(t, msg)
}
