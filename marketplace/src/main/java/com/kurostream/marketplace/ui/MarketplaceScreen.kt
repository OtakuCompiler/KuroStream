package com.kurostream.marketplace.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
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
import coil.compose.AsyncImage
import com.kurostream.domain.extension.ExtensionType
import com.kurostream.marketplace.viewmodel.MarketplaceViewModel

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
                text = "Marketplace",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Skin Pass banner
        if (!uiState.hasSkinsPass) {
            SkinPassBanner(onClick = { viewModel.claimFreeItem("skins_pass") })
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Active skin indicator
        uiState.activeSkinId?.let { activeSkinId ->
            Text(
                text = "Active Skin: $activeSkinId",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }

        // Marketplace grid
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(uiState.items, key = { it.item.id }) { item ->
                MarketplaceItemCard(
                    item = item,
                    onClick = {
                        if (item.canPurchase) {
                            val url = viewModel.getCheckoutUrl(item.item.id)
                            // Open web checkout
                        } else if (item.isOwned && !item.isActive) {
                            viewModel.setActiveSkin(item.item.skinId ?: item.item.id)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SkinPassBanner(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(16.dp)
                        .align(Alignment.Center),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock All Skins",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                    text = "Get the Skins Pass for unlimited access to all themes",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                )
            }
            Button(onClick = onClick, modifier = Modifier.height(48.dp)) {
                Text("Get Free Pass", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MarketplaceItemCard(
    item: com.kurostream.marketplace.viewmodel.MarketplaceItem,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isSkin = item.item.type == "skin"
    val isPass = item.item.type == "pass"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.item.previewImageUrl ?: "",
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.item.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp),
                    )
                } else if (item.isOwned) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Owned",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(
                text = item.item.description ?: "",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isPass) {
                Text(
                    text = "Skins Pass",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            } else {
                Text(
                    text = "\$${item.item.price}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        }
        if (item.canPurchase) {
            Button(onClick = onClick, modifier = Modifier.padding(start = 16.dp)) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get")
            }
        } else if (item.isOwned && !item.isActive) {
            Button(onClick = onClick, modifier = Modifier.padding(start = 16.dp)) {
                Text("Activate")
            }
        } else if (item.isActive) {
            Text("Active", color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
        } else {
            Text("Owned", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp))
        }
    }
}