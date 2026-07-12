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

package com.kurostream.plugin.sdk.isolation

import com.kurostream.core.common.result.Result

/**
 * Interface for handling extension crashes in an isolated manner.
 * Implementations decide whether to restart, disable, or report the extension.
 */
interface CrashIsolationHandler {
    /**
     * Called when an extension throws an uncaught exception.
     * @return Result.Success if handled, Result.Error if escalation needed.
     */
    suspend fun handleCrash(extensionId: String, throwable: Throwable): Result<CrashAction>

    /**
     * Observes whether an extension is currently in a crash loop.
     */
    fun isInCrashLoop(extensionId: String): Boolean

    /**
     * Resets crash counters for an extension (e.g., after update).
     */
    suspend fun resetCrashState(extensionId: String)
}

enum class CrashAction {
    RESTART_EXTENSION,
    DISABLE_EXTENSION,
    KILL_SANDBOX,
    REPORT_AND_CONTINUE
}

/**
 * Default handler that disables an extension after 3 crashes in 5 minutes.
 */
class DefaultCrashIsolationHandler : CrashIsolationHandler {
    private val crashLog = mutableMapOf<String, MutableList<Long>>()

    override suspend fun handleCrash(extensionId: String, throwable: Throwable): Result<CrashAction> {
        val now = System.currentTimeMillis()
        val crashes = crashLog.getOrPut(extensionId) { mutableListOf() }
        crashes.add(now)
        crashes.removeAll { now - it > 300_000 } // Keep only last 5 minutes

        return Result.Success(if (crashes.size >= 3) CrashAction.DISABLE_EXTENSION else CrashAction.RESTART_EXTENSION)
    }

    override fun isInCrashLoop(extensionId: String): Boolean {
        val now = System.currentTimeMillis()
        return (crashLog[extensionId]?.count { now - it < 300_000 } ?: 0) >= 3
    }

    override suspend fun resetCrashState(extensionId: String) {
        crashLog.remove(extensionId)
    }
}
