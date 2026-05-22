package com.kurostream.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kurostream.ui.screens.home.HomeScreen
import com.kurostream.ui.screens.player.PlayerScreen
import com.kurostream.ui.screens.search.SearchScreen
import com.kurostream.ui.screens.settings.SettingsScreen
import com.kurostream.ui.screens.detail.DetailScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{contentId}") {
        fun createRoute(contentId: String) = "detail/$contentId"
    }
    object Player : Screen("player/{streamUrl}/{title}") {
        fun createRoute(streamUrl: String, title: String) =
            "player/${android.net.Uri.encode(streamUrl)}/${android.net.Uri.encode(title)}"
    }
}

@Composable
fun KuroNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onContentClick = { contentId ->
                    navController.navigate(Screen.Detail.createRoute(contentId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onContentClick = { contentId ->
                    navController.navigate(Screen.Detail.createRoute(contentId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
            DetailScreen(
                contentId = contentId,
                onPlayClick = { streamUrl, title ->
                    navController.navigate(Screen.Player.createRoute(streamUrl, title))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("streamUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val streamUrl = android.net.Uri.decode(
                backStackEntry.arguments?.getString("streamUrl") ?: ""
            )
            val title = android.net.Uri.decode(
                backStackEntry.arguments?.getString("title") ?: ""
            )
            PlayerScreen(
                streamUrl = streamUrl,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
