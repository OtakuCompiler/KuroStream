package com.kurostream.desktop.data

import androidx.compose.runtime.Stable

@Stable
class DesktopFavorites(private val cache: DesktopCache) {
    // Reads/writes are kept simple. Real cloud sync happens via KuroCloud
    // (see app/src/main/java/com/kurostream/app/sync) — desktop talks to
    // the same backend through the shared `:domain` module.
}

@Stable
class DesktopHistory(private val cache: DesktopCache) {
    // Same — cloud-synced.
}
