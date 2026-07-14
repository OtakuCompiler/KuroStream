package com.kurostream.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScopeInstance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsMemory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.PorterDuffMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.kurostream.common.memory.UnifiedMemoryManager
import com.kurostream.common.memory.MemoryState

@Composable
fun LowMemoryWarning(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val memoryManager = UnifiedMemoryManager.getInstance(context)
    val memoryState by memoryManager.memoryState.collectAsStateWithLifecycle()
    
    val isVisible = memoryState.isCritical || memoryState.isLowMemory
    
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
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            val warningData = when {
                memoryState.isCritical -> LowMemoryWarningData(
                    bgColor = Color(0xFFB71C1C),
                    textColor = Color.White,
                    iconColor = Color.White,
                    message = "CRITICAL: ${memoryState.availableMemoryMb}MB free! App may crash."
                )
                memoryState.isLowMemory -> LowMemoryWarningData(
                    bgColor = Color(0xFFC62828),
                    textColor = Color.White,
                    iconColor = Color.White,
                    message = "LOW MEMORY: ${memoryState.availableMemoryMb}MB free. Close other apps."
                }
                else -> LowMemoryWarningData(
                    bgColor = Color(0xFFF57F17),
                    textColor = Color.White,
                    iconColor = Color.White,
                    message = "Memory pressure: ${memoryState.availableMemoryMb}MB free"
                )
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            IconButton(
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
}

private data class LowMemoryWarningData(
    val bgColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val iconColor: androidx.compose.ui.graphics.Color,
    val message: String
)

@Composable
fun MemoryStatusBar(
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val context = LocalContext.current
    val memoryManager = UnifiedMemoryManager.getInstance(context)
    val memoryState by memoryManager.memoryState.collectAsStateWithLifecycle()
    
    val availableMB = memoryState.availableMemoryMb
    val totalMB = memoryState.totalMemoryMb
    val usedMB = totalMB - availableMB
    val usedPercent = if (totalMB > 0) (usedMB * 100 / totalMB) else 0
    
    val (bgColor, textColor, icon) = when {
        memoryState.isCritical -> Color(0xFFB71C1C) to Color.White to Icons.Default.Error
        memoryState.isLowMemory -> Color(0xFFC62828) to Color.White to Icons.Default.Warning
        memoryState.memoryPressure > 0.75f -> Color(0xFFF57F17) to Color.White to Icons.Default.WarningAmber
        memoryState.memoryPressure > 0.5f -> Color(0xFF2E7D32) to Color.White to Icons.Default.Info
        else -> Color(0xFF1B5E20) to Color.White to Icons.Default.Memory
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = bgColor,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = if (showDetails) Arrangement.SpaceBetween 
                                   else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            text = "${getPressureDescription(memoryState)} • ${availableMB}MB free",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Text(
                            text = "Pressure: ${(memoryState.memoryPressure * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            if ((memoryState.isCritical || memoryState.isLowMemory) && showDetails) {
                Text(
                    text = "⚠",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
            }
        }
    }
}

private fun getPressureDescription(state: MemoryState): String {
    return when {
        state.isCritical -> "CRITICAL"
        state.isLowMemory -> "LOW"
        state.memoryPressure > 0.75f -> "HIGH"
        state.memoryPressure > 0.5f -> "MODERATE"
        else -> "NORMAL"
    }
}

@Composable
fun MemoryDiagnosticsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val memoryManager = UnifiedMemoryManager.getInstance(context)
    val memoryState by memoryManager.memoryState.collectAsStateWithLifecycle()
    
    val availableMemory = memoryState.availableMemoryMb.toLong() * 1024 * 1024
    val totalMemory = memoryState.totalMemoryMb.toLong() * 1024 * 1024
    val usedMemory = totalMemory - availableMemory
    val usedPercent = if (totalMemory > 0) (usedMemory * 100 / totalMemory) else 0
    
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Memory Diagnostics", style = MaterialTheme.typography.displaySmall)
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when {
                memoryState.isCritical -> Color(0xFFB71C1C)
                memoryState.isLowMemory -> Color(0xFFC62828)
                memoryState.memoryPressure > 0.75f -> Color(0xFFF57F17)
                memoryState.memoryPressure > 0.5f -> Color(0xFF2E7D32)
                else -> Color(0xFF1B5E20)
            },
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            memoryState.isCritical -> Icons.Default.Error
                            memoryState.isLowMemory -> Icons.Default.Warning
                            memoryState.memoryPressure > 0.75f -> Icons.Default.WarningAmber
                            memoryState.memoryPressure > 0.5f -> Icons.Default.Info
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Pressure level",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Column {
                        Text(
                            text = "Memory Pressure: ${getPressureDescription(memoryState)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = getPressureDescription(memoryState),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Usage: ${formatBytes(usedMemory)} / ${formatBytes(totalMemory)} (${usedPercent}%)", 
                     style = MaterialTheme.typography.bodyLarge)
                Text("${formatBytes(availableMemory)} free", style = MaterialTheme.typography.bodyMedium, 
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Box(
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
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { StatCard("Available Memory", formatBytes(availableMemory), Icons.Default.Memory) }
            item { StatCard("Used Memory", formatBytes(usedMemory), Icons.Default.Storage) }
            item { StatCard("Total Memory", formatBytes(totalMemory), Icons.Default.DataUsage) }
            item { StatCard("Pressure", "${(memoryState.memoryPressure * 100).roundToInt()}%", Icons.Default.Warning) }
            item { StatCard("Headroom", "${memoryState.headroomMb}MB", Icons.Default.SettingsMemory) }
            
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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