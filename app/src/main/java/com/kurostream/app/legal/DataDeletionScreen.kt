package com.kurostream.app.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DataDeletionScreen(
    onDeleteAllData: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(48.dp)) {
        Text("Data Deletion", style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
        Text("This will permanently delete all local data including watch history, favorites, and settings.", modifier = Modifier.padding(vertical = 24.dp))
        Button(onClick = onDeleteAllData) {
            Text("Delete All My Data")
        }
    }
}
