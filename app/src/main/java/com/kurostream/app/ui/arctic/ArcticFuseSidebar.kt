// This file is part of KuroStream.
//
// ArcticFuseSidebar — collapsible left rail with clock, weather, hub nav,
// and profile. Matches Arctic Fuse sidebar proportions: 60dp collapsed,
// 200dp expanded, charcoal background with cyan active accent.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

enum class ArcticHub(val label: String) {
    Home("HOME"),
    Search("SEARCH"),
    Library("LIBRARY"),
    History("HISTORY"),
    Movies("MOVIES"),
    TVShows("TV SHOWS"),
    Anime("ANIME"),
    Favorites("FAVORITES"),
    YouTube("YOUTUBE"),
    Addons("ADD-ONS"),
    Debrid("DEBRID"),
    Backup("BACKUP"),
    System("SYSTEM"),
}

@Composable
fun ArcticFuseSidebar(
    activeHub: ArcticHub,
    onNavigate: (ArcticHub) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    weatherTempC: String? = null,
    profileInitial: String = "U",
    profileName: String = "User",
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    val width by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (expanded) AFSidebar.expandedWidth else AFSidebar.collapsedWidth,
        animationSpec = tween(AFMotion.fast),
        label = "sidebarWidth",
    )

    Row(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(AFBgSidebar)
            .border(width = 1.dp, color = AFBorder),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SidebarClock(
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )

            WeatherRow(
                expanded = expanded,
                tempC = weatherTempC,
            )

            NavList(
                activeHub = activeHub,
                expanded = expanded,
                onNavigate = onNavigate,
                onFocusItem = { if (!expanded) expanded = true },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            ProfileRow(
                expanded = expanded,
                initial = profileInitial,
                name = profileName,
            )
        }
    }
}

@Composable
private fun SidebarClock(expanded: Boolean, onToggle: () -> Unit) {
    var now by remember { mutableStateOf(formatNow()) }
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE MMM d", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000L)
            now = formatNow()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AFSidebar.headerHeight)
            .padding(horizontal = AFSpacing.px3)
            .border(width = 1.dp, color = AFBorder),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Text(
                    text = now.first,
                    color = AFText,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
                )
                Text(
                    text = now.second,
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                )
            }
        }

        SidebarIconButton(
            onClick = onToggle,
            tint = AFTextDim,
        ) {
            IconMenu()
        }
    }
}

@Composable
private fun WeatherRow(expanded: Boolean, tempC: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AFSidebar.weatherHeight)
            .padding(horizontal = AFSpacing.px3)
            .border(width = 1.dp, color = AFBorder),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        // weather sun icon
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconStar(tint = AFTeal, iconSize = 20.dp)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Spacer(Modifier.width(AFSpacing.px2))
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = "${tempC ?: "21"}°C",
                color = AFTextSec,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NavList(
    activeHub: ArcticHub,
    expanded: Boolean,
    onNavigate: (ArcticHub) -> Unit,
    onFocusItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        listOf(
            ArcticHub.Home,
            ArcticHub.Search,
            ArcticHub.Library,
            ArcticHub.History,
            ArcticHub.Movies,
            ArcticHub.TVShows,
            ArcticHub.Anime,
            ArcticHub.Favorites,
            ArcticHub.YouTube,
            ArcticHub.Addons,
            ArcticHub.Debrid,
            ArcticHub.Backup,
            ArcticHub.System,
        )
    }

    Column(modifier = modifier.padding(vertical = AFSpacing.px2)) {
        items.forEach { hub ->
            NavItem(
                label      = hub.label,
                icon       = { hubIcon(hub) },
                isActive   = activeHub == hub,
                expanded   = expanded,
                onFocus    = onFocusItem,
                onClick    = { onNavigate(hub) },
            )
        }
    }
}

@Composable
private fun hubIcon(hub: ArcticHub) {
    when (hub) {
        ArcticHub.Home     -> IconHome()
        ArcticHub.Search   -> IconSearch()
        ArcticHub.Library  -> IconLibrary()
        ArcticHub.History  -> IconClock()
        ArcticHub.Movies   -> IconLibrary()
        ArcticHub.TVShows  -> IconTV()
        ArcticHub.Anime    -> IconStar(tint = AFText)
        ArcticHub.Favorites-> IconFav()
        ArcticHub.YouTube  -> IconYouTube()
        ArcticHub.Addons   -> IconExtensions()
        ArcticHub.Debrid   -> IconZap()
        ArcticHub.Backup   -> IconSave()
        ArcticHub.System   -> IconSettings()
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: @Composable () -> Unit,
    isActive: Boolean,
    expanded: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) onFocus()
    }

    // Active state (spec §4.2): tinted indigo bg, 3dp left accent border,
    // rounded corner on the right side only.
    val rowColor = when {
        isActive -> AFCyan.copy(alpha = 0.12f)
        isFocused -> Color.White.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isActive -> AFCyan
        isFocused -> AFText
        else -> AFTextDim
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AFSidebar.navItemHeight)
            .clip(RoundedCornerShape(topEnd = AFRadius.md, bottomEnd = AFRadius.md))
            .background(rowColor)
            .drawBehind {
                if (isActive) {
                    drawRect(
                        color = AFCyan,
                        size = androidx.compose.ui.geometry.Size(width = 3.dp.toPx(), height = size.height),
                    )
                }
            }
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyUp && event.key == Key.Enter -> { onClick(); true }
                    event.type == KeyEventType.KeyUp && event.key == Key.NumPadEnter -> { onClick(); true }
                    event.type == KeyEventType.KeyUp && event.key == Key.DirectionCenter -> { onClick(); true }
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = AFSpacing.px3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            icon()
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row {
                Spacer(Modifier.width(AFSpacing.px3))
                Text(
                    text = label,
                    color = contentColor,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(expanded: Boolean, initial: String, name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AFSidebar.profileHeight)
            .padding(AFSpacing.px3)
            .border(width = 1.dp, color = AFBorder),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AFTeal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = AFBgDeep,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row {
                Spacer(Modifier.width(AFSpacing.px2))
                Column {
                    Text(
                        text = name,
                        color = AFText,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "Profile",
                        color = AFTextDim,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarIconButton(
    onClick: () -> Unit,
    tint: Color,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides tint,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .focusRequester(fr)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .clickable(onClick = onClick)
                .border(
                    width = if (focused) 1.dp else 0.dp,
                    color = AFCyan,
                    shape = RoundedCornerShape(AFRadius.sm),
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private fun formatNow(): Pair<String, String> {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val date = SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(Date())
    return time to date
}
