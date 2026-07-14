package com.kurostream.torrent.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment

@Composable
fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .wrapContentSize()
    )
}

@Composable
fun DropdownMenuButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.End
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024 -> "${bytesPerSec} B/s"
        bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
        else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
    }
}

fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        seconds < 86400 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        else -> "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
    }
}
