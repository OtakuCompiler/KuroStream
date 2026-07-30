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
import androidx.compose.animation.ExperimentalAnimationApi

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kurostream.app.player.PlayerScreen
import com.kurostream.app.player.PlayerViewModel
import com.kurostream.app.ui.screens.addons.AddonsScreen
import com.kurostream.app.ui.screens.details.DetailsScreen
import com.kurostream.app.ui.screens.home.HomeScreen
import com.kurostream.app.ui.screens.favorites.FavoritesScreen
import com.kurostream.app.ui.screens.history.HistoryScreen
import com.kurostream.app.ui.screens.library.LibraryScreen
import com.kurostream.app.ui.screens.search.SearchScreen
import com.kurostream.app.ui.screens.settings.SettingsScreen
import com.kurostream.app.ui.screens.settings.SourceLockSettingsScreen
// import com.kurostream.app.ui.screens.torrents.TorrentsScreen
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
                onSettingsClick = {
                    navController.navigate(SettingsRoute)
                },
                onAddonsClick = {
                    navController.navigate(AddonsRoute)
                },
                // Torrents disabled — module excluded
                onTorrentsClick = {},
                onBackupClick = {
                    navController.navigate(BackupRoute)
                },
                onFavoritesClick = { navController.navigate(FavoritesRoute) },
                onHistoryClick = { navController.navigate(HistoryRoute) },
                onLibraryClick = { navController.navigate(LibraryRoute) }
            )
        }

        composable<DetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailsRoute>()
            DetailsScreen(
                mediaId = route.mediaId,
                onBack = { navController.popBackStack() },
                onPlay = { mediaId ->
                    navController.navigate(PlayerRoute(mediaId = mediaId, episodeId = null, startPositionMs = 0L))
                },
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(DetailsRoute(mediaId))
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<AddonsRoute> {
            AddonsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<PlayerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PlayerRoute>()
            val viewModel: com.kurostream.app.player.PlayerViewModel = hiltViewModel()
            LaunchedEffect(route.mediaId) {
                viewModel.preparePlayback(route.mediaId, route.episodeId, route.startPositionMs)
            }
            com.kurostream.app.player.PlayerScreen(
                viewModel = viewModel,
                onBackPressed = { navController.popBackStack() },
            )
        }

        composable<SourceLockSettingsRoute> {
            SourceLockSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // TorrentsScreen disabled — torrent module excluded (471 pre-existing errors)
        // composable<TorrentsRoute> {
        //     TorrentsScreen(
        //         onBackClick = { navController.popBackStack() }
        //     )
        // }

        composable<BackupRoute> {
            BackupSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<FavoritesRoute> {
            FavoritesScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<HistoryRoute> {
            HistoryScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<LibraryRoute> {
            LibraryScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}