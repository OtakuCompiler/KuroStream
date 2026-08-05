// This file is part of KuroStream.
//
// Arctic Fuse skeleton loaders — rectangle shimmering contents matching
// the Arctic Fuse Kodi skin loading state visuals.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SkeletonBase = Color(0xFF1F2833)
private val SkeletonHi = Color(0xFF2A3441)

/**
 * Single shimmering rectangle.
 */
@Composable
fun ArcticSkeleton(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = AFRadius.md,
) {
    val transition = rememberInfiniteTransition(label = "af-skeleton")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "af-skeleton-phase",
    )

    val gradient = Brush.horizontalGradient(
        colors = listOf(SkeletonBase, SkeletonHi, SkeletonBase),
        startX = -1000f + phase * 1000f,
        endX = phase * 1000f,
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient),
    )
}

/**
 * Shimmering row of poster-style cards. Mirrors Arctic Fuse CardSkeleton.
 */
@Composable
fun ArcticCardSkeleton(
    count: Int = 6,
    modifier: Modifier = Modifier,
    view: CardView = CardView.Poster,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AFSpacing.safeZoneH),
        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px4),
    ) {
        repeat(count) {
            ArcticSingleCardSkeleton(view = view)
        }
    }
}

@Composable
private fun ArcticSingleCardSkeleton(view: CardView) {
    val w = when (view) {
        CardView.Poster -> AFCardSize.posterWidth
        CardView.Landscape -> AFCardSize.landscapeWidth
        CardView.Episode -> AFCardSize.episodeWidth
    }
    val h = when (view) {
        CardView.Poster -> AFCardSize.posterHeight
        CardView.Landscape -> AFCardSize.landscapeHeight
        CardView.Episode -> AFCardSize.episodeHeight
    }
    Column {
        ArcticSkeleton(
            modifier = Modifier.size(width = w, height = h),
            cornerRadius = AFRadius.lg,
        )
        Spacer(modifier = Modifier.height(AFSpacing.px2))
        ArcticSkeleton(
            modifier = Modifier.width(if (view == CardView.Poster) 96.dp else 160.dp).height(16.dp),
        )
        Spacer(modifier = Modifier.height(AFSpacing.px1))
        ArcticSkeleton(
            modifier = Modifier.width(if (view == CardView.Poster) 64.dp else 96.dp).height(12.dp),
        )
    }
}

/**
 * Hero skeleton with title + plot contents. Mirrors Arctic Fuse HeroSkeleton.
 */
@Composable
fun ArcticHeroSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AFHero.height)
            .background(AFSurface)
            .padding(AFSpacing.safeZoneH),
        contentAlignment = androidx.compose.ui.Alignment.BottomStart,
    ) {
        Column {
            ArcticSkeleton(
                modifier = Modifier.width(260.dp).height(28.dp),
            )
            Spacer(modifier = Modifier.height(AFSpacing.px3))
            ArcticSkeleton(
                modifier = Modifier.width(360.dp).height(16.dp),
            )
            Spacer(modifier = Modifier.height(AFSpacing.px2))
            ArcticSkeleton(
                modifier = Modifier.width(180.dp).height(16.dp),
            )
        }
    }
}

/**
 * Full-page skeleton loader for Arctic Fuse detail screen.
 * Shows shimmering poster + text blocks in the Arctic Fuse layout.
 */
@Composable
fun ArcticFuseSkeletonPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AFBg)
            .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.safeZoneV),
    ) {
        // Back button content
        ArcticSkeleton(modifier = Modifier.size(48.dp, 36.dp), cornerRadius = AFRadius.sm)
        Spacer(modifier = Modifier.height(AFSpacing.px6))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AFSpacing.px6),
        ) {
            // Poster skeleton
            ArcticSkeleton(
                modifier = Modifier.size(width = AFCardSize.posterWidth, height = AFCardSize.posterHeight),
                cornerRadius = AFRadius.lg,
            )
            // Metadata skeletons
            Column(modifier = Modifier.weight(1f)) {
                ArcticSkeleton(modifier = Modifier.width(400.dp).height(32.dp), cornerRadius = AFRadius.sm)
                Spacer(modifier = Modifier.height(AFSpacing.px3))
                ArcticSkeleton(modifier = Modifier.width(200.dp).height(20.dp), cornerRadius = AFRadius.sm)
                Spacer(modifier = Modifier.height(AFSpacing.px3))
                repeat(3) {
                    ArcticSkeleton(
                        modifier = Modifier.fillMaxWidth(0.8f).height(16.dp),
                        cornerRadius = AFRadius.sm,
                    )
                    Spacer(modifier = Modifier.height(AFSpacing.px2))
                }
                Spacer(modifier = Modifier.height(AFSpacing.px4))
                // Buttons skeleton
                Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3)) {
                    ArcticSkeleton(modifier = Modifier.size(120.dp, 48.dp), cornerRadius = AFRadius.md)
                    ArcticSkeleton(modifier = Modifier.size(120.dp, 48.dp), cornerRadius = AFRadius.md)
                }
            }
        }
    }
}

/**
 * Full-page error state for Arctic Fuse screens.
 * Shows error message with retry and back options.
 */
@Composable
fun ArcticFuseErrorPage(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AFBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Error icon
            Text(
                text = "!",
                color = AFDanger,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(AFSpacing.px4))
            Text(
                text = "Something went wrong",
                color = AFText,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(AFSpacing.px3))
            Text(
                text = message,
                color = AFTextSec,
                fontSize = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = AFSpacing.px12),
            )
            Spacer(modifier = Modifier.height(AFSpacing.px8))
            Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px4)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AFRadius.md))
                        .background(AFSurface)
                        .clickable { onRetry() }
                        .padding(horizontal = AFSpacing.px6, vertical = AFSpacing.px3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Retry", color = AFCyan, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AFRadius.md))
                        .background(AFSurface)
                        .clickable { onBack() }
                        .padding(horizontal = AFSpacing.px6, vertical = AFSpacing.px3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Back", color = AFText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

enum class CardView { Poster, Landscape, Episode }
