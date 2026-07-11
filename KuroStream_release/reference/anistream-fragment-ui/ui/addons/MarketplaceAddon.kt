package com.kurostream.legacyui.anistream.ui.addons

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MarketplaceAddon(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val rating: Float,
    val downloadCount: Int,
    val iconUrl: String?,
    val screenshotUrls: List<String>,
    val permissions: List<String>,
    val sourceUrl: String,
    val isOfficial: Boolean = false
) : Parcelable {
    fun toAddon(): com.kurostream.data.anistream.addons.Addon = com.kurostream.data.anistream.addons.Addon(
        id = id,
        name = name,
        version = version,
        description = description,
        author = author,
        iconUrl = iconUrl,
        sourceUrl = sourceUrl,
        isOfficial = isOfficial,
        permissions = permissions
    )
}
