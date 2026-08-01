package com.kurostream.app.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    versionCode: Int,
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit,
    onOpenSourceLicenses: () -> Unit,
    onDataDeletion: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(48.dp)) {
        Text("About KuroStream", style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
        Text("Version $versionName ($versionCode)", modifier = Modifier.padding(top = 16.dp))
        Text("Licensed under GPL-3.0", modifier = Modifier.padding(top = 8.dp))
        Text("Built with love for the streaming community", modifier = Modifier.padding(top = 8.dp))
    }
}
