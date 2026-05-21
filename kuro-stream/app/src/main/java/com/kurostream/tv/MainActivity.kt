package com.kurostream.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.kurostream.tv.navigation.KuroStreamNavGraph
import com.kurostream.tv.ui.theme.KuroStreamTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Kuro Stream TV App.
 * 
 * Handles the app entry point and sets up Compose navigation.
 * Optimized for TV with D-pad navigation support.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Keep splash screen visible while loading initial data
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        setContent {
            KuroStreamTheme {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground
                ) {
                    KuroStreamNavGraph(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        onAppReady = {
                            keepSplashOnScreen = false
                        }
                    )
                }
            }
        }
    }

    /**
     * Handle back button press with TV-optimized behavior.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Let the navigation component handle back navigation first
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
