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
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kurostream.app.player.PlayerActivity
import com.kurostream.app.navigation.TvNavHost
import com.kurostream.app.ui.screens.splash.SplashScreen
import com.kurostream.app.ui.theme.AnimeStreamTVTheme
import com.kurostream.app.ui.theme.DynamicThemeProvider
import com.kurostream.app.ui.theme.TvDarkColorScheme
import com.kurostream.app.ui.theme.toDynamicPalette
import com.kurostream.data.local.preferences.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private var deepLinkMediaId: String? = null
    private var deepLinkEpisodeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        handleDeepLink(intent)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val defaultPalette = remember { TvDarkColorScheme.toDynamicPalette() }
            var onboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                onboardingCompleted = settingsDataStore.onboardingCompleted.first()
            }

            AnimeStreamTVTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen(
                            onTimeout = { showSplash = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (onboardingCompleted == false) {
                        val navController = rememberNavController()
                        TvNavHost(
                            navController = navController,
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
                                    val intent = PlayerActivity.createIntent(
                                        this@MainActivity,
                                        mediaId,
                                        deepLinkEpisodeId,
                                        0L
                                    )
                                    startActivity(intent)
                                    deepLinkMediaId = null
                                    deepLinkEpisodeId = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
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
