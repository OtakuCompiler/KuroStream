package com.kurostream.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kurostream.data.model.StreamSource
import com.kurostream.ui.components.FocusableCard
import com.kurostream.ui.components.LoadingScreen
import com.kurostream.ui.theme.*

@Composable
fun DetailScreen(
    contentId: String,
    onPlayClick: (streamUrl: String, title: String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contentId) {
        viewModel.loadContent(contentId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroBackground)
    ) {
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorState(uiState.error!!, onBack)
            uiState.content != null -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        BackdropHeader(
                            backdropUrl = uiState.content?.backdrop ?: uiState.content?.poster,
                            title = uiState.content?.title ?: "",
                            rating = uiState.content?.rating,
                            year = uiState.content?.year,
                            onBack = onBack
                        )
                    }
                    item {
                        ContentInfo(
                            description = uiState.content?.description,
                            genres = uiState.content?.genres ?: emptyList(),
                            cast = uiState.content?.cast ?: emptyList()
                        )
                    }
                    if (uiState.streams.isNotEmpty()) {
                        item {
                            StreamsSection(
                                streams = uiState.streams,
                                onStreamClick = { stream ->
                                    onPlayClick(stream.url, uiState.content?.title ?: "")
                                }
                            )
                        }
                    } else if (uiState.isLoadingStreams) {
                        item {
                            LoadingSection("Finding streams...")
                        }
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BackdropHeader(
    backdropUrl: String?,
    title: String,
    rating: Double?,
    year: Int?,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, KuroBackground)
                    )
                )
        )
        FocusableCard(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.padding(10.dp))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                year?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodyMedium, color = KuroOnSurfaceVariant)
                }
                rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f".format(it), style = MaterialTheme.typography.bodyMedium, color = KuroOnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentInfo(
    description: String?,
    genres: List<String>,
    cast: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (genres.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres) { genre ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = KuroPrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelLarge,
                            color = KuroPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        description?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = KuroOnSurfaceVariant)
        }
    }
}

@Composable
private fun StreamsSection(
    streams: List<StreamSource>,
    onStreamClick: (StreamSource) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Available Streams", style = MaterialTheme.typography.titleLarge, color = KuroOnSurface)
        streams.forEach { stream ->
            FocusableCard(
                onClick = { onStreamClick(stream) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stream.title, style = MaterialTheme.typography.bodyLarge, color = KuroOnSurface)
                        Text(stream.pluginName, style = MaterialTheme.typography.bodySmall, color = KuroOnSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stream.resolution?.let {
                            Surface(shape = MaterialTheme.shapes.small, color = KuroAccent.copy(alpha = 0.2f)) {
                                Text(it, style = MaterialTheme.typography.labelLarge, color = KuroAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Icon(Icons.Default.PlayArrow, null, tint = KuroPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingSection(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = KuroPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = KuroOnSurfaceVariant)
    }
}

@Composable
private fun ErrorState(error: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            FocusableCard(onClick = onBack) {
                Text("Go Back", color = KuroOnSurface, modifier = Modifier.padding(12.dp))
            }
        }
    }
}
