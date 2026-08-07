package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.search.SearchResult

/**
 * Minimal poster card used across the desktop shell. Mirrors the layout
 * proportions of ArcticFuseMediaCard on Android so visuals stay consistent.
 */
@Composable
fun DesktopMediaCard(item: SearchResult, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(180.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Poster placeholder (real impl would load coil image)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF2A2A3E),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = item.title.take(1).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFFE94560),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(item.year?.toString(), item.rating?.let { "%.1f".format(it) })
                .joinToString(" • ")
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
