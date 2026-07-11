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

package com.kurostream.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kurostream.app.ui.screens.home.HomeScreen
import com.kurostream.app.ui.screens.details.DetailsScreen
import com.kurostream.app.ui.screens.search.SearchScreen
import com.kurostream.app.ui.screens.downloads.DownloadsScreen
import com.kurostream.app.ui.screens.settings.SettingsScreen
import com.kurostream.app.ui.screens.addons.AddonsScreen
import com.kurostream.torrent.ui.TorrentsScreen
import com.kurostream.backup.ui.BackupSettingsScreen

private const val NAV_ANIM_DURATION = 300

@Composable
fun TvNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(NAV_ANIM_DURATION)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(NAV_ANIM_DURATION)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(NAV_ANIM_DURATION)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(NAV_ANIM_DURATION)
            )
        }
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(DetailsRoute(mediaId))
                },
                onSearchClick = {
                    navController.navigate(SearchRoute)
                },
                onDownloadsClick = {
                    navController.navigate(DownloadsRoute)
                },
                onSettingsClick = {
                    navController.navigate(SettingsRoute)
                },
                onAddonsClick = {
                    navController.navigate(AddonsRoute)
                },
                onTorrentsClick = {
                    navController.navigate(TorrentsRoute)
                },
                onBackupClick = {
                    navController.navigate(BackupRoute)
                }
            )
        }

        composable<DetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailsRoute>()
            DetailsScreen(
                mediaId = route.mediaId,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { mediaId, episodeId ->
                    // Launch PlayerActivity
                },
                onTrailerClick = { trailerUrl ->
                },
                onRelatedClick = { relatedId ->
                    navController.navigate(DetailsRoute(relatedId))
                }
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onResultClick = { mediaId ->
                    navController.navigate(DetailsRoute(mediaId))
                }
            )
        }

        composable<DownloadsRoute> {
            DownloadsScreen(
                onBackClick = { navController.popBackStack() },
                onItemClick = { mediaId ->
                    navController.navigate(DetailsRoute(mediaId))
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }

        composable<AddonsRoute> {
            AddonsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<TorrentsRoute> {
            TorrentsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<BackupRoute> {
            BackupSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}