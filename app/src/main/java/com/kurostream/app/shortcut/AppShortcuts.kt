package com.kurostream.app.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.kurostream.app.MainActivity
import com.kurostream.app.R

object AppShortcuts {
    fun createShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        val searchShortcut = ShortcutInfo.Builder(context, "search")
            .setShortLabel("Search")
            .setLongLabel("Search KuroStream")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_media_pause))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("kurostream://search")
            })
            .build()

        val favoritesShortcut = ShortcutInfo.Builder(context, "favorites")
            .setShortLabel("Favorites")
            .setLongLabel("My Favorites")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_media_pause))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("kurostream://favorites")
            })
            .build()

        shortcutManager.dynamicShortcuts = listOf(searchShortcut, favoritesShortcut)
    }
}
