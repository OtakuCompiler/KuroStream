// This file is part of KuroStream.
//
// TorrentsScreen — TV-friendly torrent streaming UI backed by the
// :torrent module's OptimizedTorrentEngine.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.torrents

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import com.kurostream.app.ui.arctic.ArcticFuseTheme
import com.kurostream.app.ui.arctic.AFBgDeep
import com.kurostream.app.ui.arctic.AFCyan
import com.kurostream.app.ui.arctic.AFText
import com.kurostream.app.ui.arctic.AFTextDim

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TorrentsScreen(
    onBackClick: () -> Unit = {},
    viewModel: TorrentsViewModel = hiltViewModel(),
) {
    val torrents by viewModel.torrents.collectAsStateWithLifecycle()
    val isStarted by viewModel.isStarted.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var magnetInput by remember { mutableStateOf("") }

    ArcticFuseTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AFBgDeep)
                .padding(48.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Torrent Streaming",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AFText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AFText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isStarted) "Engine running" else "Engine starting...",
                color = if (isStarted) AFCyan else AFTextDim,
                fontSize = 14.sp,
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = magnetInput,
                    onValueChange = { magnetInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Paste magnet link...") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        viewModel.addMagnet(magnetInput)
                        magnetInput = ""
                    },
                ) {
                    Text("Add Torrent")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (torrents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No torrents yet. Paste a magnet link to start streaming.", color = AFTextDim)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(torrents, key = { it.magnet }) { torrent ->
                        TorrentRow(
                            torrent = torrent,
                            onRemove = { viewModel.removeTorrent(torrent.magnet) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TorrentRow(
    torrent: TorrentUi,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = torrent.name.ifBlank { torrent.magnet },
                color = AFText,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { torrent.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${torrent.progress}% • ${torrent.state}",
                color = AFTextDim,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onRemove) {
            Text("Stop")
        }
    }
}
