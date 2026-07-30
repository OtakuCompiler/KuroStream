package com.kurostream.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.kurostream.app.ui.theme.TvPrimary

/**
 * A single item in the sidebar navigation.
 */
data class SidebarItem(
    val id: String,
    val label: String,
    val icon: @Composable () -> Unit,
    val route: String,
)

/**
 * NuvioTV-style sidebar navigation rail.
 *
 * Collapsed (64dp): shows only icons.
 * Expanded (200dp): shows icons + labels with a logo header.
 */
@Composable
fun SidebarNavigation(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 64.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "sidebarWidth",
    )

    val items = remember {
        listOf(
            SidebarItem("home", "Home", { Text("\uD83C\uDFE0", fontSize = 20.sp) }, "home"),
            SidebarItem("search", "Search", { Text("\uD83D\uDD0D", fontSize = 20.sp) }, "search"),
            SidebarItem("settings", "Settings", { Text("\u2699\uFE0F", fontSize = 20.sp) }, "settings"),
            SidebarItem("addons", "Add-ons", { Text("\uD83E\uDDE9", fontSize = 20.sp) }, "addons"),
            SidebarItem("torrents", "Torrents", { Text("\u26A1", fontSize = 20.sp) }, "torrents"),
            SidebarItem("backup", "Backup", { Text("\uD83D\uDCBE", fontSize = 20.sp) }, "backup"),
            SidebarItem("favorites", "Favorites", { Text("❤", fontSize = 20.sp) }, "favorites"),
            SidebarItem("history", "History", { Text("⏰", fontSize = 20.sp) }, "history"),
            SidebarItem("library", "Library", { Text("📚", fontSize = 20.sp) }, "library"),
        )
    }

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(Color(0xCC0A0A0F))
            .padding(vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Logo area ──────────────────────────────────────────────
            if (isExpanded) {
                Text(
                    text = "KuroStream",
                    color = TvPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TvPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "K",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Navigation items ───────────────────────────────────────
            items.forEach { item ->
                SidebarNavItem(
                    item = item,
                    isSelected = selectedItem == item.id,
                    isExpanded = isExpanded,
                    onClick = { onItemSelected(item.id) },
                )
            }
        }
    }
}

/**
 * A single navigation item inside the sidebar.
 * Shows an active indicator, icon, and optional label.
 * Highlights on selection and dims on focus.
 */
@Composable
private fun SidebarNavItem(
    item: SidebarItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isSelected -> TvPrimary.copy(alpha = 0.3f)
        isFocused -> Color.White.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .focusable(interactionSource = interactionSource)
            .onFocusChanged { }
            .clickable(onClick = onClick)
            .then(
                if (isExpanded) {
                    Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                } else {
                    Modifier.padding(vertical = 12.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
    ) {
        // Active indicator (selected items only)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(TvPrimary, RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Icon
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            item.icon()
        }

        // Label (visible only in expanded state)
        if (isExpanded) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.label,
                color = if (isSelected) TvPrimary else Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
