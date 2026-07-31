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

package com.kurostream.common.optimization

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import timber.log.Timber

/**
 * Network call deduplication manager
 */
class NetworkDeduplicator {
    private val inFlightRequests = mutableMapOf<String, kotlinx.coroutines.Deferred<*>>()
    private val lock = Any()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    suspend fun <T> execute(key: String, request: suspend () -> T): T {
        val existing = synchronized(lock) {
            inFlightRequests[key] as? kotlinx.coroutines.Deferred<T>
        }

        if (existing != null) {
            return existing.await()
        }

        val deferred = scope.async {
            try {
                request()
            } finally {
                synchronized(lock) {
                    inFlightRequests.remove(key)
                }
            }
        }

        synchronized(lock) {
            inFlightRequests[key] = deferred
        }

        return deferred.await()
    }

    fun shutdown() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}

/**
 * Lazy layout optimization helpers
 */
object LazyLayoutOptimizations {
    fun <T : Any> stableKey(item: T): Any = item

    fun <T> generateStableKeys(items: List<T>, keySelector: (T) -> Any): List<Any> {
        return items.map(keySelector)
    }
}

/**
 * Paging 3 configuration for large lists
 */
object PagingConfig {
    const val PAGE_SIZE = 20
    const val PREFETCH_DISTANCE = 10
    const val INITIAL_LOAD_SIZE = 30
    const val MAX_SIZE = 200
    const val ENABLE_PLACEHOLDERS = false
}

