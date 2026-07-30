package com.kurostream.app.ui.screens.debrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.kurostream.data.debrid.DebridService

@Composable
fun DebridSetupScreen(
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var selectedProvider by remember { mutableStateOf<DebridService.DebridProvider?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Debrid Services", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Text("Unlock premium sources. No subscription required.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        DebridService.DebridProvider.values().forEach { provider ->
            OutlinedCard(
                onClick = { selectedProvider = provider },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(provider.displayName, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Text("Click to sign up via our affiliate link", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }

        selectedProvider?.let { provider ->
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { uriHandler.openUri(provider.signupUrl) }) {
                Text("Sign Up for ${provider.displayName}")
            }
        }
    }
}
