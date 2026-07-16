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

package com.kurostream.app.startup

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.startup.Initializer
import com.kurostream.common.thermal.ThermalGuard
import com.kurostream.app.player.PlayerInitializer
import com.kurostream.app.repository.SyncInitializer
import com.kurostream.app.extensions.PluginScannerInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber

/**
 * App Startup initializer for KuroStream.
 * Uses IdleHandler to defer non-critical initialization until after first frame.
 */
class KuroStreamInitializer : Initializer<Unit> {

    override fun create(context: Context): Unit {
        // Critical: Initialize thermal monitoring early
        ThermalGuard.getInstance(context)

        // Defer everything else to idle handler (after first frame)
        Looper.myQueue().addIdleHandler {
            supervisorScope {
                initPluginSdk(context)
                initFirebase(context)
                initSyncManager(context)
                initPreCacheManager(context)
                Timber.d("KuroStream deferred initialization complete")
            }
            false // Run once
        }

        // Second-level deferral for lowest priority tasks
        Handler(Looper.getMainLooper()).postDelayed({
            supervisorScope {
                initLowPriority(context)
            }
        }, 5000)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        PluginScannerInitializer::class.java,
        PlayerInitializer::class.java,
        SyncInitializer::class.java,
    )

    private suspend fun initPluginSdk(context: Context) {
        try {
            // Plugin SDK initialization would go here
            // com.kurostream.plugin.sdk.manager.ExtensionManager.getInstance(context).initialize()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize plugin SDK")
        }
    }

    private suspend fun initFirebase(context: Context) {
        try {
            // Firebase is auto-initialized, but we can do additional setup here
            // com.google.firebase.FirebaseApp.initializeApp(context)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
        }
    }

    private suspend fun initSyncManager(context: Context) {
        try {
            // Sync manager initialization
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize sync manager")
        }
    }

    private suspend fun initPreCacheManager(context: Context) {
        try {
            // Pre-cache manager initialization
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize pre-cache manager")
        }
    }

    private suspend fun initLowPriority(context: Context) {
        try {
            // Lowest priority initialization
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize low priority components")
        }
    }
}

    private fun initSyncManager(context: Context) {
        try {
            // Sync manager initialization
            // com.kurostream.launcher.firebase.firestore.FirestoreSyncManager.getInstance(context).initialize()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize sync manager")
        }
    }

    private fun initPreCacheManager(context: Context) {
        try {
            // Predictive pre-caching initialization
            // com.kurostream.launcher.cache.PreCacheWorker.schedulePreCache(context)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize pre-cache manager")
        }
    }

    private fun initLowPriority(context: Context) {
        try {
            com.kurostream.common.util.StringInterner.preloadCommonStrings()
            com.kurostream.common.memory.LowRamDevice.initialize(context)
            // Pre-allocate buffer pool on ample-RAM devices only
            if (com.kurostream.common.memory.LowRamDevice.bufferPoolPreallocate) {
                com.kurostream.common.pool.BufferPool.preallocate(4 * 1024 * 1024, 2)
                com.kurostream.common.pool.BufferPool.preallocate(8 * 1024 * 1024, 1)
            } else {
                com.kurostream.common.pool.BufferPool.setLowRamMode(true)
            }
            Timber.d("KuroStream low-priority initialization complete")
        } catch (e: Exception) {
            Timber.e(e, "Failed low-priority initialization")
        }
    }
}