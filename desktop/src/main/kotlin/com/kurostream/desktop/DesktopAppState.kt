/*
 * DesktopAppState — application-wide singleton for the desktop app.
 * Mirrors the role of Hilt's @Singleton graph on Android, but without DI.
 * Holds the search/history/favorites/cache repos and the player factory.
 */
package com.kurostream.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import com.kurostream.desktop.data.DesktopCache
import com.kurostream.desktop.data.DesktopFavorites
import com.kurostream.desktop.data.DesktopHistory
import com.kurostream.desktop.data.DesktopSettings
import com.kurostream.desktop.playback.DesktopPlayerFactory
import com.kurostream.desktop.search.DesktopSearchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File

class DesktopAppState private constructor(
    val settings: DesktopSettings,
    val cache: DesktopCache,
    val favorites: DesktopFavorites,
    val history: DesktopHistory,
    val search: DesktopSearchService,
    val playerFactory: DesktopPlayerFactory,
    val scope: CoroutineScope,
) {
    fun shutdown() {
        scope.cancel()
        cache.close()
        playerFactory.releaseAll()
    }

    companion object {
        @OptIn(ExperimentalComposeUiApi::class)
        fun create(): DesktopAppState {
            val appDataDir = locateDataDir()
            val cache = DesktopCache.open(appDataDir.resolve("cache.db"))
            val settings = DesktopSettings.load(appDataDir.resolve("settings.json"))
            val favorites = DesktopFavorites(cache)
            val history = DesktopHistory(cache)
            val search = DesktopSearchService(
                settings = settings,
                cache = cache,
            )
            val playerFactory = DesktopPlayerFactory()
            return DesktopAppState(
                settings = settings,
                cache = cache,
                favorites = favorites,
                history = history,
                search = search,
                playerFactory = playerFactory,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            )
        }

        private fun locateDataDir(): File {
            val os = System.getProperty("os.name").lowercase()
            val home = System.getProperty("user.home")
            val dir = when {
                os.contains("win") -> File(System.getenv("APPDATA") ?: "$home/AppData/Roaming")
                    .resolve("KuroStream")
                os.contains("mac") -> File("$home/Library/Application Support")
                    .resolve("KuroStream")
                else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share")
                    .resolve("kurostream")
            }
            dir.mkdirs()
            return dir
        }
    }
}
