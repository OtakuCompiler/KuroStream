package com.kurostream.app.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.app.ui.arctic.AFBg
import com.kurostream.app.ui.arctic.AFCyan
import com.kurostream.app.ui.arctic.AFSurface
import com.kurostream.app.ui.theme.TvBackground
import com.kurostream.app.ui.theme.TvSurface
import com.kurostream.app.ui.theme.TvPrimary

/**
 * Download item data model.
 */
data class DownloadItem(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float,
    val status: DownloadStatus,
    val size: String,
)

enum class DownloadStatus { DOWNLOADING, PAUSED, COMPLETED, FAILED }

/**
 * DownloadsScreen — manage and monitor downloads.
 */
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDownloads()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AFBg)
    ) {
        // ===== HEADER =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFE0E0E0)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Downloads",
                color = Color(0xFFE0E0E0),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== CONTENT =====
        when (val state = uiState) {
            is DownloadsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        color = Color(0xFFE0E0E0).copy(alpha = 0.5f),
                        fontSize = 18.sp
                    )
                }
            }

            is DownloadsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 18.sp
                    )
                }
            }

            is DownloadsUiState.Success -> {
                if (state.downloads.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No downloads yet",
                            color = Color(0xFFE0E0E0).copy(alpha = 0.5f),
                            fontSize = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.downloads, key = { it.id }) { item ->
                            DownloadItemCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(item: DownloadItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AFSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (item.status) {
                        DownloadStatus.DOWNLOADING -> AFCyan.copy(alpha = 0.2f)
                        DownloadStatus.COMPLETED -> Color.Green.copy(alpha = 0.2f)
                        DownloadStatus.PAUSED -> Color.Yellow.copy(alpha = 0.2f)
                        DownloadStatus.FAILED -> Color.Red.copy(alpha = 0.2f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (item.status) {
                    DownloadStatus.DOWNLOADING -> "${(item.progress * 100).toInt()}%"
                    DownloadStatus.COMPLETED -> "✓"
                    DownloadStatus.PAUSED -> "⏸"
                    DownloadStatus.FAILED -> "✗"
                },
                color = when (item.status) {
                    DownloadStatus.DOWNLOADING -> AFCyan
                    DownloadStatus.COMPLETED -> Color.Green
                    DownloadStatus.PAUSED -> Color.Yellow
                    DownloadStatus.FAILED -> Color.Red
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color(0xFFE0E0E0),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.description} • ${item.size}",
                color = Color(0xFFE0E0E0).copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}