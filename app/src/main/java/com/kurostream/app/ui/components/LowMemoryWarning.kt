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

package com.kurostream.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.PorterDuffMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.kurostream.common.memory.MemoryMonitor
import com.kurostream.common.memory.MemoryPressure

/**
 * Low memory warning indicator - shows in corner when memory is critically low.
 */
@Composable
fun LowMemoryWarning(
    memoryPressure: StateFlow<MemoryPressure> = MemoryMonitor.getInstance(LocalContext.current).memoryPressure,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val pressure by memoryPressure.collectAsStateWithLifecycle()
    val isVisible = pressure == MemoryPressure.CRITICAL || pressure == MemoryPressure.EMERGENCY
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300)
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(300)
    )
    
    AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.fadeIn(animationSpec = tween(300)) +
                androidx.compose.animation.scaleIn(animationSpec = tween(300)),
        exit = androidx.compose.animation.fadeOut(animationSpec = tween(200)) +
               androidx.compose.animation.scaleOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = animatedAlpha
                    scaleX = animatedScale
                    scaleY = animatedScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                }
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            val warningData = when (pressure) {
                MemoryPressure.EMERGENCY -> {
                    LowMemoryWarningData(
                        bgColor = Color(0xFFB71C1C),
                        textColor = Color.White,
                        iconColor = Color.White,
                        message = "CRITICAL: ${getAvailableMB()}MB free! App may crash."
                    )
                }
                MemoryPressure.CRITICAL -> {
                    LowMemoryWarningData(
                        bgColor = Color(0xFFC62828),
                        textColor = Color.White,
                        iconColor = Color.White,
                        message = "LOW MEMORY: ${getAvailableMB()}MB free. Close other apps."
                    )
                }
                else -> {
                    LowMemoryWarningData(
                        bgColor = Color(0xFFF57F17),
                        textColor = Color.White,
                        iconColor = Color.White,
                        message = "Memory pressure: ${getAvailableMB()}MB free"
                    )
                }
            }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = warningData.bgColor.copy(alpha = 0.95f),
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Memory warning",
                            tint = warningData.iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Column {
                            Text(
                                text = "Memory Warning",
                                style = MaterialTheme.typography.titleSmall,
                                color = warningData.textColor
                            )
                            Text(
                                text = warningData.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = warningData.textColor.copy(alpha = 0.9f),
                                maxLines = 2,
                                textAlign = TextAlign.Start
                            )
                        }
                        
                        if (onDismiss != null) {
                            androidx.tv.material3.IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = warningData.textColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    private data class LowMemoryWarningData(
        val bgColor: androidx.compose.ui.graphics.Color,
        val textColor: androidx.compose.ui.graphics.Color,
        val iconColor: androidx.compose.ui.graphics.Color,
        val message: String
    )

    private fun getAvailableMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024)
    }
}

@Composable
fun MemoryStatusBar(
    memoryPressure: StateFlow<MemoryPressure> = MemoryMonitor.getInstance(LocalContext.current).memoryPressure,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val pressure by memoryPressure.collectAsStateWithLifecycle()
    val availableMB = getAvailableMB()
    
    val (bgColor, textColor, icon) = when (pressure) {
        MemoryPressure.EMERGENCY -> Color(0xFFB71C1C) to Color.White to Icons.Default.Error
        MemoryPressure.CRITICAL -> Color(0xFFC62828) to Color.White to Icons.Default.Warning
        MemoryPressure.HIGH -> Color(0xFFF57F17) to Color.White to Icons.Default.WarningAmber
        MemoryPressure.MODERATE -> Color(0xFF2E7D32) to Color.White to Icons.Default.Memory
        MemoryPressure.NORMAL -> Color(0xFF1B5E20) to Color.White to Icons.Default.Memory
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        color = bgColor,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = if (showDetails) androidx.compose.foundation.layout.Arrangement.SpaceBetween 
                                   else androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Memory status",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                
                if (showDetails) {
                    Column {
                        Text(
                            text = "${pressure.description} • ${availableMB}MB free",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Text(
                            text = "Pressure: ${pressure.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            if (pressure != MemoryPressure.NORMAL && showDetails) {
                Text(
                    text = "⚠",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
            }
        }
    }
}

private fun getAvailableMB(): Long {
    val runtime = Runtime.getRuntime()
    return (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024)
}

/**
 * Detailed memory diagnostics screen component.
 */
@Composable
fun MemoryDiagnosticsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val memoryMonitor = com.kurostream.common.memory.MemoryMonitor.getInstance(context)
    val pressure by memoryMonitor.memoryPressure.collectAsStateWithLifecycle()
    val availableMemory by memoryMonitor.availableMemory.collectAsStateWithLifecycle()
    val totalMemory by memoryMonitor.totalMemory.collectAsStateWithLifecycle()
    val trimCount by memoryMonitor.trimCallbackCount.collectAsStateWithLifecycle()
    val lastTrimLevel by memoryMonitor.lastTrimLevel.collectAsStateWithLifecycle()
    
    val usedMemory = totalMemory - availableMemory
    val usedPercent = if (totalMemory > 0) (usedMemory * 100 / totalMemory) else 0
    
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Memory Diagnostics", style = MaterialTheme.typography.displaySmall)
            androidx.tv.material3.IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        
        // Pressure indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when (pressure) {
                MemoryPressure.EMERGENCY -> Color(0xFFB71C1C)
                MemoryPressure.CRITICAL -> Color(0xFFC62828)
                MemoryPressure.HIGH -> Color(0xFFF57F17)
                MemoryPressure.MODERATE -> Color(0xFF2E7D32)
                MemoryPressure.NORMAL -> Color(0xFF1B5E20)
            },
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (pressure) {
                            MemoryPressure.EMERGENCY -> Icons.Default.Error
                            MemoryPressure.CRITICAL -> Icons.Default.Warning
                            MemoryPressure.HIGH -> Icons.Default.WarningAmber
                            MemoryPressure.MODERATE -> Icons.Default.Info
                            MemoryPressure.NORMAL -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Pressure level",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Column {
                        Text(
                            text = "Memory Pressure: ${pressure.name}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = pressure.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        
        // Memory usage bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text("Usage: ${formatBytes(usedMemory)} / ${formatBytes(totalMemory)} (${usedPercent}%)", 
                     style = MaterialTheme.typography.bodyLarge)
                Text("${formatBytes(availableMemory)} free", style = MaterialTheme.typography.bodyMedium, 
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth(usedPercent / 100f)
                        .height(24.dp)
                        .background(
                            when {
                                usedPercent > 90 -> Color(0xFFB71C1C)
                                usedPercent > 75 -> Color(0xFFC62828)
                                usedPercent > 60 -> Color(0xFFF57F17)
                                else -> Color(0xFF2E7D32)
                            }
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                )
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        
        // Stats grid
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            item { StatCard("Available Memory", formatBytes(availableMemory), Icons.Default.Memory) }
            item { StatCard("Used Memory", formatBytes(usedMemory), Icons.Default.Storage) }
            item { StatCard("Total Memory", formatBytes(totalMemory), Icons.Default.DataUsage) }
            item { StatCard("Trim Callbacks", trimCount.toString(), Icons.Default.Refresh) }
            item { StatCard("Last Trim Level", lastTrimLevel.toString(), Icons.Default.SettingsMemory) }
            
            // Java heap stats
            val runtime = Runtime.getRuntime()
            val heapMax = runtime.maxMemory()
            val heapTotal = runtime.totalMemory()
            val heapFree = runtime.freeMemory()
            val heapUsed = heapTotal - heapFree
            
            item { StatCard("Java Heap Max", formatBytes(heapMax), Icons.Default.DataUsage) }
            item { StatCard("Java Heap Used", formatBytes(heapUsed), Icons.Default.Storage) }
            item { StatCard("Java Heap Free", formatBytes(heapFree), Icons.Default.FreeBreakfast) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, 
                 tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, 
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, 
                     color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)} GB"
        bytes >= 1024L * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}