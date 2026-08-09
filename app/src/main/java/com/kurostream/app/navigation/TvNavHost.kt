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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kurostream.app.player.PlayerActivity
import com.kurostream.app.ui.screens.addons.AddonsScreen
import com.kurostream.app.ui.screens.debrid.DebridSetupScreen
import com.kurostream.app.ui.screens.details.DetailsScreen
import com.kurostream.app.ui.screens.extensions.ExtensionConfigScreen
import com.kurostream.app.ui.screens.extensions.ExtensionsScreen
import com.kurostream.app.ui.screens.home.HomeScreen
import com.kurostream.app.ui.screens.favorites.FavoritesScreen
import com.kurostream.app.ui.screens.history.HistoryScreen
import com.kurostream.app.ui.screens.library.LibraryScreen
import com.kurostream.app.ui.screens.search.SearchScreen
import com.kurostream.app.ui.screens.settings.SettingsScreen
import com.kurostream.app.ui.screens.settings.SourceLockSettingsScreen
import com.kurostream.app.ui.screens.torrents.TorrentsScreen
import com.kurostream.app.ui.screens.onboarding.OnboardingScreen
import com.kurostream.backup.ui.BackupSettingsScreen
import com.kurostream.marketplace.ui.MarketplaceScreen as KuroStoreScreen

private const val NAV_ANIM_DURATION = 300

@Composable
fun TvNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // System back: navigate up if possible, else finish at Home (default Compose behavior)
    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(NAV_ANIM_DURATION),
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(NAV_ANIM_DURATION),
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(NAV_ANIM_DURATION),
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(NAV_ANIM_DURATION),
            )
        },
    ) {
        composable<HomeRoute> {
            val ctx = LocalContext.current
            HomeScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onPlayClick = { item ->
                    PlayerActivity.createIntent(ctx, item.id, null, 0L).also {
                        ctx.startActivity(it)
                    }
                },
                onSearchClick = { navController.navigate(SearchRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
                onAddonsClick = { navController.navigate(AddonsRoute) },
                onTorrentsClick = { navController.navigate(TorrentsRoute) },
                onFavoritesClick = { navController.navigate(FavoritesRoute) },
                onHistoryClick = { navController.navigate(HistoryRoute) },
                onLibraryClick = { navController.navigate(LibraryRoute) },
            )
        }

        composable<DetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailsRoute>()
            val context = LocalContext.current
            DetailsScreen(
                mediaId = route.mediaId,
                onBack = { navController.popBackStack() },
                onPlay = { mediaId ->
                    PlayerActivity.createIntent(context, mediaId, null, 0L).also {
                        context.startActivity(it)
                    }
                },
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onClose = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onMarketplaceClick = { navController.navigate(MarketplaceRoute) },
                onExtensionsClick = { navController.navigate(AddonsRoute) },
                onSourceLockClick = { navController.navigate(SourceLockSettingsRoute) },
                onPlaybackClick = { navController.navigate(PlaybackSettingsRoute) },
                onPrivacyClick = { navController.navigate(PrivacySettingsRoute) },
                onDisplayClick = { navController.navigate(DisplaySettingsRoute) },
                onNetworkClick = { navController.navigate(NetworkSettingsRoute) },
                onAccountClick = { navController.navigate(AccountSettingsRoute) },
                onStorageClick = { navController.navigate(StorageSettingsRoute) },
                onDebridClick = { navController.navigate(DebridRoute) },
                onAboutClick = { navController.navigate(AboutRoute) },
                onDiagnosticsClick = { navController.navigate(DiagnosticsRoute) },
                onBackupClick = { navController.navigate(BackupRoute) },
                onTorrentsClick = { navController.navigate(TorrentsRoute) },
                onTraktClick = { navController.navigate(TraktRoute) },
                onRealDebridClick = { navController.navigate(DebridRoute) },
                onAllDebridClick = { navController.navigate(DebridRoute) },
                onOpenSubtitlesClick = { navController.navigate(OpenSubtitlesRoute) },
                onTmdbClick = { navController.navigate(TmdbRoute) },
                onTvdbClick = { navController.navigate(TvdbRoute) },
                onKitsuClick = { navController.navigate(KitsuRoute) },
                onSimklClick = { navController.navigate(SimklRoute) },
                onAniListClick = { navController.navigate(AniListRoute) },
                onMalClick = { navController.navigate(MalRoute) },
            )
        }

        composable<AddonsRoute> {
            AddonsScreen(
                onBackClick = { navController.popBackStack() },
                onMarketplaceClick = { navController.navigate(MarketplaceRoute) },
                onConfigure = { id -> navController.navigate(ExtensionConfigRoute(id)) },
            )
        }

        composable<ExtensionsRoute> {
            ExtensionsScreen(
                onBackClick = { navController.popBackStack() },
                onConfigure = { id -> navController.navigate(ExtensionConfigRoute(id)) },
            )
        }

        composable<SourceLockSettingsRoute> {
            SourceLockSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<TorrentsRoute> {
            TorrentsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<MarketplaceRoute> {
            KuroStoreScreen(onBack = { navController.popBackStack() })
        }

        composable<BackupRoute> {
            BackupSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<FavoritesRoute> {
            FavoritesScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<HistoryRoute> {
            HistoryScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<LibraryRoute> {
            LibraryScreen(
                onMediaClick = { mediaId -> navController.navigate(DetailsRoute(mediaId)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<ExtensionConfigRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ExtensionConfigRoute>()
            ExtensionConfigScreen(
                extensionId = route.extensionId,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<DebridRoute> {
            DebridSetupScreen(onBack = { navController.popBackStack() })
        }

        composable<OnboardingRoute> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )
        }

        composable<PlaybackSettingsRoute> {
            com.kurostream.app.ui.screens.settings.PlaybackSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PrivacySettingsRoute> {
            com.kurostream.app.ui.screens.settings.PrivacySettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<DisplaySettingsRoute> {
            com.kurostream.app.ui.screens.settings.DisplaySettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<NetworkSettingsRoute> {
            com.kurostream.app.ui.screens.settings.NetworkSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<AccountSettingsRoute> {
            com.kurostream.app.ui.screens.settings.AccountSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<StorageSettingsRoute> {
            com.kurostream.app.ui.screens.settings.StorageSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<AboutRoute> {
            com.kurostream.app.ui.screens.settings.AboutScreen(onBackClick = { navController.popBackStack() })
        }
        composable<DiagnosticsRoute> {
            com.kurostream.app.ui.screens.settings.DiagnosticsScreen(onBackClick = { navController.popBackStack() })
        }
        composable<TraktRoute> {
            com.kurostream.app.ui.screens.settings.TraktConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<OpenSubtitlesRoute> {
            com.kurostream.app.ui.screens.settings.OpenSubtitlesConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<TmdbRoute> {
            com.kurostream.app.ui.screens.settings.TmdbConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<TvdbRoute> {
            com.kurostream.app.ui.screens.settings.TvdbConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<KitsuRoute> {
            com.kurostream.app.ui.screens.settings.KitsuConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<SimklRoute> {
            com.kurostream.app.ui.screens.settings.SimklConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<AniListRoute> {
            com.kurostream.app.ui.screens.settings.AniListConfigScreen(onBackClick = { navController.popBackStack() })
        }
        composable<MalRoute> {
            com.kurostream.app.ui.screens.settings.MalConfigScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
