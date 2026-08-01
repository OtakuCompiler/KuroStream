package com.kurostream.app.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import timber.log.Timber

class CastManager(context: Context) {

    private val castContext = CastContext.getSharedInstance(context)
    private val sessionManager = castContext.sessionManager

    fun addSessionListener(listener: SessionManagerListener<CastSession>) {
        sessionManager.addSessionManagerListener(listener, CastSession::class.java)
    }

    fun removeSessionListener(listener: SessionManagerListener<CastSession>) {
        sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
    }

    fun getCurrentSession(): CastSession? = sessionManager.currentCastSession

    fun endSession() {
        sessionManager.endCurrentSession(true)
    }

    fun isConnected(): Boolean = sessionManager.currentCastSession?.isConnected == true
}