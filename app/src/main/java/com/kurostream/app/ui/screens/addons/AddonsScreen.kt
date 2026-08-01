// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.screens.addons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kurostream.app.ui.arctic.AFBg
import com.kurostream.app.ui.arctic.AFBorder
import com.kurostream.app.ui.arctic.AFCyan
import com.kurostream.app.ui.arctic.AFSurface
import com.kurostream.app.ui.arctic.AFSurfaceHighlight

/**
 * Addon data model representing a content source or plugin.
 */
data class AddonItem(
    val id: String,
    val name: String,
    val description: String,
    val category: AddonCategory,
    val isInstalled: Boolean,
    val iconUrl: String = "",
    val version: String = "1.0.0",
    val author: String = "Community",
    val url: String = "",
)

/**
 * Categories for organizing add-ons.
 */
enum class AddonCategory { STREMIO, KITSU, COMMUNITY, INSTALLED }

/**
 * AddonsScreen — browse, install, and manage content source add-ons.
 *
 * Features:
 * - Category tabs: Installed, Available (Stremio), Available (Kitsu), Community
 * - Grid of add-on cards with smooth focus animations (scale + border)
 * - Each card shows name, description, author, version, and action button
 * - Handles loading, empty, installed, and available states
 */
@Composable
fun AddonsScreen(
    onBackClick: () -> Unit,
    viewModel: AddonsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Filter add-ons based on selected tab
    val filteredAddons = remember(uiState, uiState.selectedCategory) {
        when (uiState.selectedCategory) {
            AddonCategory.INSTALLED -> uiState.installedAddons
            else -> uiState.availableAddons.filter { it.category == uiState.selectedCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AFBg)
            .padding(32.dp),
    ) {
        // ===== HEADER =====
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Column {
                Text(
                    text = "Add-ons",
                    color = Color(0xFFE0E0E0),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Install and manage content sources",
                    color = Color(0xFFE0E0E0).copy(alpha = 0.6f),
                    fontSize = 16.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== CATEGORY TABS =====
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(AddonCategory.entries) { category ->
                CategoryTab(
                    title = category.name,
                    isSelected = uiState.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== LOADING STATE =====
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AFCyan)
            }
        } else if (uiState.error != null) {
            // ===== ERROR STATE =====
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = Color(0xFFE0E0E0).copy(alpha = 0.6f),
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AFCyan)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .clickable { viewModel.refreshAddons() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Retry",
                            color = Color(0xFFE0E0E0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        } else if (filteredAddons.isEmpty()) {
            // ===== EMPTY STATE =====
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (uiState.selectedCategory == AddonCategory.INSTALLED) {
                        "No installed add-ons"
                    } else {
                        "No add-ons available in this category"
                    },
                    color = Color(0xFFE0E0E0).copy(alpha = 0.4f),
                    fontSize = 18.sp,
                )
            }
        } else {
            // ===== ADD-ON GRID =====
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = filteredAddons,
                    key = { it.id },
                ) { addon ->
                    AddonCard(
                        addon = addon,
                        onInstallClick = { viewModel.installAddon(addon.id) },
                        onConfigureClick = { viewModel.configureAddon(addon.id) },
                        onUninstallClick = { viewModel.uninstallAddon(addon.id) },
                    )
                }
            }
        }
    }
}

/**
 * A selectable category tab for the add-on filter bar.
 */
@Composable
private fun CategoryTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AFCyan else AFSurface)
            .focusable()
            .onFocusChanged { }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = if (isSelected) Color(0xFFE0E0E0) else Color(0xFFE0E0E0).copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * AddonCard — A card displaying an add-on with icon, info, and action button.
 *
 * Focus animations: scale 1.0→1.02 with spring, border highlight on focus.
 */
@Composable
private fun AddonCard(
    addon: AddonItem,
    onInstallClick: () -> Unit,
    onConfigureClick: () -> Unit,
    onUninstallClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 2f else 0f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "cardBorderWidth",
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "cardScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) AFSurfaceHighlight else AFSurface)
            .border(
                width = borderWidth.dp,
                color = AFBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .focusable()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    if (addon.isInstalled) {
                        onConfigureClick()
                    } else {
                        onInstallClick()
                    }
                    true
                } else {
                    false
                }
            }
            .clickable {
                if (addon.isInstalled) {
                    onConfigureClick()
                } else {
                    onInstallClick()
                }
            }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ===== ICON content =====
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AFCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = addon.name.take(2).uppercase(),
                color = AFCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ===== INFO SECTION =====
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = addon.name,
                color = Color(0xFFE0E0E0),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = addon.description,
                color = Color(0xFFE0E0E0).copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${addon.author} · v${addon.version}",
                color = Color(0xFFE0E0E0).copy(alpha = 0.4f),
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ===== ACTION BUTTON =====
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (addon.isInstalled) Color(0xFFE0E0E0).copy(alpha = 0.1f)
                    else AFCyan
                )
                .clickable {
                    if (addon.isInstalled) {
                        onConfigureClick()
                    } else {
                        onInstallClick()
                    }
                }
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = if (addon.isInstalled) "Configure" else "Install",
                color = Color(0xFFE0E0E0),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}