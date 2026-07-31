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

package com.kurostream.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kurostream.app.navigation.TvNavHost
import com.kurostream.app.ui.screens.splash.SplashScreen
import com.kurostream.app.ui.theme.AnimeStreamTVTheme
import com.kurostream.app.ui.theme.DynamicThemeProvider
import com.kurostream.app.ui.theme.TvDarkColorScheme
import com.kurostream.app.ui.theme.toDynamicPalette
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var deepLinkMediaId: String? = null
    private var deepLinkEpisodeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val defaultPalette = remember { TvDarkColorScheme.toDynamicPalette() }

            AnimeStreamTVTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen(
                            onTimeout = { showSplash = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DynamicThemeProvider(
                            palette = defaultPalette,
                            isAmoled = false,
                        ) {
                            val navController = rememberNavController()
                            TvNavHost(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )

                            LaunchedEffect(Unit) {
                                deepLinkMediaId?.let { mediaId ->
                                    val route = com.kurostream.app.navigation.PlayerRoute(
                                        mediaId = mediaId,
                                        episodeId = deepLinkEpisodeId,
                                        startPositionMs = 0L
                                    )
                                    navController.navigate(route)
                                    deepLinkMediaId = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        when (uri.host) {
            "play" -> {
                val mediaId = uri.getQueryParameter("id") ?: return
                deepLinkMediaId = mediaId
                deepLinkEpisodeId = uri.getQueryParameter("episode")
            }
            "details" -> {
                val mediaId = uri.getQueryParameter("id") ?: return
                deepLinkMediaId = mediaId
                deepLinkEpisodeId = uri.getQueryParameter("episode")
            }
        }
    }
}
