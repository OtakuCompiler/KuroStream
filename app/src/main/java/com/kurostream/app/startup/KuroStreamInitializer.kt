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
import androidx.startup.Initializer
import com.kurostream.common.thermal.ThermalGuard
import com.kurostream.app.player.PlayerInitializer
import com.kurostream.app.repository.SyncInitializer
import com.kurostream.app.extensions.PluginScannerInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App Startup initializer for KuroStream.
 * Defines initialization order for heavy components to run after first draw.
 */
class KuroStreamInitializer : Initializer<Unit> {

    override fun create(context: Context): Unit {
        // Initialize thermal monitoring early
        ThermalGuard.initialize(context)

        // Deferred initialization after first draw
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize plugin SDK
            initPluginSdk(context)

            // Initialize Firebase if not already done
            initFirebase(context)

            // Initialize sync manager
            initSyncManager(context)

            // Initialize predictive pre-caching
            initPreCacheManager(context)

            Timber.d("KuroStream deferred initialization complete")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        PluginScannerInitializer::class.java,
        PlayerInitializer::class.java,
        SyncInitializer::class.java,
    )

    private fun initPluginSdk(context: Context) {
        try {
            // Plugin SDK initialization would go here
            // com.kurostream.plugin.sdk.manager.ExtensionManager.getInstance(context).initialize()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize plugin SDK")
        }
    }

    private fun initFirebase(context: Context) {
        try {
            // Firebase is auto-initialized, but we can do additional setup here
            // com.google.firebase.FirebaseApp.initializeApp(context)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
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
}