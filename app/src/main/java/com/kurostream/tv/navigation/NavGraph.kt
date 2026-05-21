package com.kurostream.tv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kurostream.tv.ui.detail.AnimeDetailScreen
import com.kurostream.tv.ui.discover.DiscoverScreen
import com.kurostream.tv.ui.home.HomeScreen
import com.kurostream.tv.ui.mylist.MyListScreen
import com.kurostream.tv.ui.player.PlayerScreen
import com.kurostream.tv.ui.settings.SettingsScreen
import com.kurostream.tv.ui.search.SearchScreen
import com.kurostream.tv.ui.auth.PinLockScreen
import com.kurostream.tv.ui.anilist.AniListTabScreen

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Discover : Screen("discover")
    data object MyList : Screen("mylist")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
    data object PinLock : Screen("pin_lock")
    data object AniList : Screen("anilist")
    
    data object AnimeDetail : Screen("anime/{animeId}") {
        fun createRoute(animeId: String) = "anime/$animeId"
    }
    
    data object Player : Screen("player/{animeId}/{episodeNumber}") {
        fun createRoute(animeId: String, episodeNumber: Int) = "player/$animeId/$episodeNumber"
    }
}

/**
 * Main navigation graph for Kuro Stream.
 * 
 * Implements TV-optimized navigation with:
 * - D-pad friendly focus management
 * - Smooth transitions between screens
 * - Deep linking support
 */
@Composable
fun KuroStreamNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    onAppReady: () -> Unit = {}
) {
    // Signal app ready after first composition
    LaunchedEffect(Unit) {
        onAppReady()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        // Home Screen - Featured anime, continue watching, recommendations
        composable(Screen.Home.route) {
            HomeScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onNavigateToDiscover = {
                    navController.navigate(Screen.Discover.route)
                },
                onNavigateToMyList = {
                    navController.navigate(Screen.MyList.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onPlayEpisode = { animeId, episodeNumber ->
                    navController.navigate(Screen.Player.createRoute(animeId, episodeNumber))
                }
            )
        }

        // Discover Screen - Browse and filter anime
        composable(Screen.Discover.route) {
            DiscoverScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // My List Screen - User's watchlist and library
        composable(Screen.MyList.route) {
            MyListScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onBackPressed = {
                    navController.popBackStack()
                },
                onNavigateToAniList = {
                    navController.navigate(Screen.AniList.route)
                }
            )
        }

        // Settings Screen - App configuration
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onNavigateToPinLock = {
                    navController.navigate(Screen.PinLock.route)
                }
            )
        }

        // Search Screen - Global search
        composable(Screen.Search.route) {
            SearchScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // PIN Lock Screen - Profile protection
        composable(Screen.PinLock.route) {
            PinLockScreen(
                onPinVerified = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // AniList Integration Screen
        composable(Screen.AniList.route) {
            AniListTabScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        // Anime Detail Screen
        composable(
            route = Screen.AnimeDetail.route,
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: return@composable
            
            AnimeDetailScreen(
                animeId = animeId,
                onPlayEpisode = { id, episodeNumber ->
                    navController.navigate(Screen.Player.createRoute(id, episodeNumber))
                },
                onBackPressed = {
                    navController.popBackStack()
                },
                onRelatedAnimeClick = { relatedAnimeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(relatedAnimeId))
                }
            )
        }

        // Player Screen
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("episodeNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: return@composable
            val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: return@composable
            
            PlayerScreen(
                animeId = animeId,
                episodeNumber = episodeNumber,
                onBackPressed = {
                    navController.popBackStack()
                },
                onNextEpisode = { nextEpisodeNumber ->
                    navController.navigate(Screen.Player.createRoute(animeId, nextEpisodeNumber)) {
                        popUpTo(Screen.Player.route) { inclusive = true }
                    }
                },
                onPreviousEpisode = { prevEpisodeNumber ->
                    navController.navigate(Screen.Player.createRoute(animeId, prevEpisodeNumber)) {
                        popUpTo(Screen.Player.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Default enter transition for navigation.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(300)) + slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(300)
    )
}

/**
 * Default exit transition for navigation.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(300)
    )
}

/**
 * Default pop enter transition for navigation.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(300)) + slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(300)
    )
}

/**
 * Default pop exit transition for navigation.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(300)
    )
}
