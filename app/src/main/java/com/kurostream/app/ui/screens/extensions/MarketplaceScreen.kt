package com.kurostream.app.ui.screens.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.TextField
import coil.compose.AsyncImage
import com.kurostream.domain.extension.MarketplaceItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Extension Marketplace",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var searchQuery by remember { mutableStateOf("") }
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search addons...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.search(searchQuery) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(uiState.categories, key = { it.id }) { category ->
                CategoryChip(
                    name = category.name,
                    count = category.itemCount,
                    onClick = { viewModel.filterByCategory(category.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.Gray)
            }
        } else if (uiState.items.isEmpty() && searchQuery.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No extensions found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.items, key = { it.extension.id }) { item ->
                    MarketplaceItemCard(
                        item = item,
                        onInstall = { viewModel.install(item.extension) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryChip(name: String, count: Int, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("$name ($count)")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MarketplaceItemCard(
    item: MarketplaceItem,
    onInstall: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f))
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.extension.iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.extension.name,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.extension.description,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.avgRating} • ${item.installCount} installs • ${item.extension.author}",
                    color = Color.Gray,
                    fontSize = 11.sp,
                )
            }
        }
        Button(onClick = onInstall) {
            Icon(Icons.Default.Download, contentDescription = "Install")
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (item.isInstalled) "Update" else "Install")
        }
    }
}
