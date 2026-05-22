package com.kurostream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.ui.theme.KuroBackground
import com.kurostream.ui.theme.KuroOnSurfaceVariant
import com.kurostream.ui.theme.KuroPrimary

@Composable
fun LoadingScreen(message: String = "Loading...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = KuroPrimary, strokeWidth = 3.dp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = KuroOnSurfaceVariant
            )
        }
    }
}

@Composable
fun NoPluginsScreen(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Text(
                text = "No plugins installed",
                style = MaterialTheme.typography.headlineSmall,
                color = com.kurostream.ui.theme.KuroOnSurface
            )
            Text(
                text = "Add a plugin from Settings to start streaming content.",
                style = MaterialTheme.typography.bodyMedium,
                color = KuroOnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FocusableCard(
                onClick = onSettingsClick,
                containerColor = KuroPrimary
            ) {
                Text(
                    text = "Open Settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}
