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

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
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
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it / 3 })
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
            slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { it / 4 })
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
            slideInHorizontally(animationSpec = tween(200), initialOffsetX = { it / 3 })
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it / 4 })
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
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}

@Composable
fun SlideFadeTransition(
    content: @Composable () -> Unit,
    direction: SlideDirection = SlideDirection.HORIZONTAL,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}

enum class SlideDirection { HORIZONTAL, VERTICAL }

@Composable
fun SharedElementTransition(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    content()
}
