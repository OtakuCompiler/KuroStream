// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kurostream.app.ui.arctic.AFMotion
import com.kurostream.app.ui.arctic.AFRadius
import com.kurostream.app.ui.arctic.AFSpacing
import com.kurostream.app.ui.arctic.AFTypo
import com.kurostream.app.ui.arctic.IconCheck
import com.kurostream.app.ui.arctic.IconClose
import com.kurostream.app.ui.arctic.LocalArcticFusePalette
import com.kurostream.app.ui.theme.CustomTheme
import com.kurostream.app.ui.theme.CustomThemeEngine

/**
 * Full-screen custom theme editor.
 *
 * Allows the user to:
 * 1. Pick a preset as a starting point.
 * 2. Fine-tune individual colors (accent, background, text, etc.) via
 *    hue/saturation/value sliders and a color swatch picker.
 * 3. Preview the theme live (all colors react instantly).
 * 4. Save or reset.
 */
@Composable
fun CustomThemeScreen(
    engine:  CustomThemeEngine,
    visible: Boolean,
    onClose: () -> Unit,
) {
    val palette  = LocalArcticFusePalette.current
    val theme    by engine.theme.collectAsState()

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(AFMotion.normal)),
        exit    = fadeOut(tween(AFMotion.fast)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.safeZoneV),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Custom Theme", color = palette.text,
                                fontSize = AFTypo.heading, fontWeight = FontWeight.Bold)
                            Text("Personalise every colour", color = palette.textSec,
                                fontSize = AFTypo.body)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceVariant)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center,
                        ) { IconClose(tint = palette.text, iconSize = 20.dp) }
                    }
                }

                // Preset gallery
                item {
                    ThemeSection(title = "Presets", palette = palette)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(CustomTheme.PRESETS) { preset ->
                            PresetCard(
                                preset    = preset,
                                selected  = theme.name == preset.name,
                                onClick   = { engine.applyPreset(preset) },
                            )
                        }
                    }
                }

                // Color pickers
                item { ThemeSection("Accent (Primary)", palette) }
                item { ColorRow(label = "Hue", color = Color(theme.accentPrimary)) { r, g, b ->
                    engine.updateAccentPrimary(Color(r, g, b))
                } }

                item { ThemeSection("Accent (Secondary)", palette) }
                item { ColorRow(label = "Hue", color = Color(theme.accentSecondary)) { r, g, b ->
                    engine.updateAccentSecondary(Color(r, g, b))
                } }

                item { ThemeSection("Background", palette) }
                item { ColorRow(label = "Lightness", color = Color(theme.background)) { r, g, b ->
                    engine.updateBackground(Color(r, g, b))
                } }

                item { ThemeSection("Card Surface", palette) }
                item { ColorRow(label = "Lightness", color = Color(theme.surface)) { r, g, b ->
                    engine.updateSurface(Color(r, g, b))
                } }

                // Live preview
                item {
                    ThemeSection("Live Preview", palette)
                    Spacer(Modifier.height(12.dp))
                    ThemePreviewCard(theme)
                }

                // Actions
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionButton("Reset to Default", palette.danger) { engine.reset() }
                        ActionButton("Save Theme", palette.cyan) { onClose() }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun ThemeSection(title: String, palette: com.kurostream.app.ui.arctic.ArcticFusePalette) {
    Text(
        text       = title.uppercase(),
        color      = palette.textDim,
        fontSize   = AFTypo.micro,
        fontWeight = FontWeight.Bold,
        letterSpacing = AFTypo.sectionTitleSpacing,
    )
}

@Composable
private fun PresetCard(preset: CustomTheme, selected: Boolean, onClick: () -> Unit) {
    val accent = Color(preset.accentPrimary)
    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(AFRadius.md))
            .background(Color(preset.surface))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color(preset.border),
                shape = RoundedCornerShape(AFRadius.md),
            )
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Column {
            // Swatch row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(Color(preset.accentPrimary))
                Swatch(Color(preset.accentSecondary))
                Swatch(Color(preset.background))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text       = preset.name,
                color      = Color(preset.textPrimary),
                fontSize   = AFTypo.meta,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
            )
            if (selected) {
                Spacer(Modifier.height(4.dp))
                IconCheck(tint = accent, iconSize = 14.dp)
            }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Simple RGB channel sliders.
 * In a real app this would be a HSV wheel — but RGB is serialization-friendly
 * and avoids complex colour-space math in Compose on TV D-pad.
 */
@Composable
private fun ColorRow(
    label: String,
    color: Color,
    onChange: (Float, Float, Float) -> Unit,
) {
    val palette = LocalArcticFusePalette.current
    var r by remember(color) { mutableStateOf(color.red) }
    var g by remember(color) { mutableStateOf(color.green) }
    var b by remember(color) { mutableStateOf(color.blue) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Current colour preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(AFRadius.sm))
                .background(Color(r, g, b)),
        )
        ColorSlider("R", r, Color(1f, 0f, 0f), palette.textSec) { r = it; onChange(r, g, b) }
        ColorSlider("G", g, Color(0f, 1f, 0f), palette.textSec) { g = it; onChange(r, g, b) }
        ColorSlider("B", b, Color(0f, 0f, 1f), palette.textSec) { b = it; onChange(r, g, b) }
    }
}

@Composable
private fun ColorSlider(
    label:     String,
    value:     Float,
    thumbColor: Color,
    textColor: Color,
    onChange:  (Float) -> Unit,
) {
    val palette = LocalArcticFusePalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = textColor, fontSize = AFTypo.meta, modifier = Modifier.width(16.dp))
        Spacer(Modifier.width(8.dp))
        Slider(
            value         = value,
            onValueChange = onChange,
            modifier      = Modifier.weight(1f),
            colors        = SliderDefaults.colors(
                thumbColor          = thumbColor,
                activeTrackColor    = thumbColor,
                inactiveTrackColor  = palette.surfaceVariant,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text("${(value * 255).toInt()}", color = textColor, fontSize = AFTypo.micro, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun ThemePreviewCard(theme: CustomTheme) {
    val bg     = Color(theme.background)
    val surf   = Color(theme.surface)
    val accent = Color(theme.accentPrimary)
    val text   = Color(theme.textPrimary)
    val textSec= Color(theme.textSecondary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(AFRadius.lg))
            .background(bg)
            .border(1.dp, Color(theme.border), RoundedCornerShape(AFRadius.lg))
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(AFRadius.sm)).background(surf))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Sample Title", color = text, fontSize = AFTypo.body, fontWeight = FontWeight.Bold)
                    Text("Subtitle text", color = textSec, fontSize = AFTypo.meta)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Fake progress bar
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(theme.surfaceVariant))) {
                Box(Modifier.fillMaxWidth(0.45f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            }
            Spacer(Modifier.height(12.dp))
            // Fake buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(RoundedCornerShape(AFRadius.sm)).background(accent).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text("Play", color = Color.White, fontSize = AFTypo.meta, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.clip(RoundedCornerShape(AFRadius.sm)).background(surf).border(1.dp, accent, RoundedCornerShape(AFRadius.sm)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text("+ List", color = accent, fontSize = AFTypo.meta)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AFRadius.sm))
            .background(color.copy(alpha = if (focused) 1f else 0.15f))
            .border(1.dp, color, RoundedCornerShape(AFRadius.sm))
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(label, color = color, fontSize = AFTypo.body, fontWeight = FontWeight.SemiBold)
    }
}
