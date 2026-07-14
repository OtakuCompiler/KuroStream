package com.kurostream.plugin.sdk.isolation

import com.kurostream.core.common.result.Result
import java.util.concurrent.ConcurrentHashMap

interface CrashIsolationHandler {
    suspend fun handleCrash(extensionId: String, throwable: Throwable): Result<CrashAction>
    fun isInCrashLoop(extensionId: String): Boolean
    suspend fun resetCrashState(extensionId: String)
}

enum class CrashAction {
    RESTART_EXTENSION,
    DISABLE_EXTENSION,
    KILL_SANDBOX,
    REPORT_AND_CONTINUE
}

class DefaultCrashIsolationHandler : CrashIsolationHandler {
    private val crashLog = ConcurrentHashMap<String, MutableList<Long>>()

    override suspend fun handleCrash(extensionId: String, throwable: Throwable): Result<CrashAction> {
        val now = System.currentTimeMillis()
        val crashes = crashLog.getOrPut(extensionId) { mutableListOf() }
        synchronized(crashes) {
            crashes.add(now)
            crashes.removeAll { now - it > 300_000 }
        }
        val crashCount = synchronized(crashes) { crashes.size }
        return Result.Success(if (crashCount >= 3) CrashAction.DISABLE_EXTENSION else CrashAction.RESTART_EXTENSION)
    }

    override fun isInCrashLoop(extensionId: String): Boolean {
        val now = System.currentTimeMillis()
        val crashes = crashLog[extensionId] ?: return false
        return synchronized(crashes) { crashes.count { now - it < 300_000 } } >= 3
    }

    override suspend fun resetCrashState(extensionId: String) {
        crashLog.remove(extensionId)
    }
}
