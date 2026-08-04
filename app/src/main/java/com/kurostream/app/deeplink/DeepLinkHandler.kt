package com.kurostream.app.deeplink

import android.content.Intent
import androidx.navigation.NavController

object DeepLinkHandler {
    fun handle(intent: Intent?, navController: NavController) {
        val data = intent?.data ?: return
        when {
            data.path?.startsWith("/details/") == true -> {
                val id = data.lastPathSegment ?: return
                navController.navigate("details/$id")
            }
            data.path?.startsWith("/player/") == true -> {
                val id = data.lastPathSegment ?: return
                navController.navigate("player/$id")
            }
            data.path == "/search" -> {
                navController.navigate("search")
            }
            data.path == "/favorites" -> {
                navController.navigate("favorites")
            }
        }
    }
}
