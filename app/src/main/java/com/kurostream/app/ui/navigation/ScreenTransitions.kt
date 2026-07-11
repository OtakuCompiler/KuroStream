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

package com.kurostream.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIntoContainer
import androidx.compose.animation.slideOutOfContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun TvNavHostWithTransitions(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onCreateNavController: (NavHostController) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { _, _ ->
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(Alignment.CenterStart, tween(300))
        },
        exitTransition = { _, _ ->
            fadeOut(animationSpec = tween(200)) +
            slideOutOfContainer(Alignment.CenterEnd, tween(200))
        },
        popEnterTransition = { _, _ ->
            fadeIn(animationSpec = tween(200)) +
            slideIntoContainer(Alignment.CenterEnd, tween(200))
        },
        popExitTransition = { _, _ ->
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(Alignment.CenterStart, tween(300))
        },
    ) {
        onCreateNavController(navController)
    }
}

@Composable
fun FadeThroughTransition(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = Unit,
        transitionSpec = {
            fadeOut(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150))
        },
        modifier = modifier,
    ) { _ ->
        content()
    }
}

@Composable
fun SlideFadeTransition(
    content: @Composable () -> Unit,
    direction: SlideDirection = SlideDirection.HORIZONTAL,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = Unit,
        enterTransition = {
            fadeIn(animationSpec = tween(250)) +
            slideIntoContainer(
                if (direction == SlideDirection.HORIZONTAL) Alignment.CenterStart else Alignment.TopCenter,
                tween(250)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
            slideOutOfContainer(
                if (direction == SlideDirection.HORIZONTAL) Alignment.CenterEnd else Alignment.BottomCenter,
                tween(200)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
            slideIntoContainer(
                if (direction == SlideDirection.HORIZONTAL) Alignment.CenterEnd else Alignment.BottomCenter,
                tween(200)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(250)) +
            slideOutOfContainer(
                if (direction == SlideDirection.HORIZONTAL) Alignment.CenterStart else Alignment.TopCenter,
                tween(250)
            )
        },
        modifier = modifier,
    ) { _ ->
        content()
    }
}

enum class SlideDirection { HORIZONTAL, VERTICAL }

@Composable
fun SharedElementTransition(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // For shared element transitions between screens
    // Would require accompanist-navigation-animation
    content()
}