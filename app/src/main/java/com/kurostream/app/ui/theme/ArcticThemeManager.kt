package com.kurostream.app.ui.theme

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import com.kurostream.data.kurocloud.sync.KuroSyncRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manager for applying active skin from KuroCloud sync to the Arctic Fuse theme.
 * Observes the sync repository for active skin changes and applies them at runtime.
 */
class ArcticThemeManager(private val context: Context) {

    private val repository: KuroSyncRepository = EntryPointAccessors.fromApplication(
        context.applicationContext,
        KuroThemeEntryPoint::class.java
    ).syncRepository()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeSkinId: String? = null

    fun startObserving(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                startThemeSync()
            }

            override fun onStop(owner: LifecycleOwner) {
                // Keep observing in background for FCM pushes
            }
        })

        // Also observe in background for FCM pushes
        startThemeSync()
    }

    private fun startThemeSync() {
        scope.launch {
            repository.entitlements
                .distinctUntilChanged()
                .collect { state ->
                    val newSkinId = when (state) {
                        is com.kurostream.domain.sync.KuroEntitlementsState.Loaded -> state.activeSkinId
                        else -> null
                    }

                    if (newSkinId != activeSkinId) {
                        activeSkinId = newSkinId
                        applySkin(newSkinId)
                    }
                }
        }
    }

    private fun applySkin(skinId: String?) {
        skinId?.let { skin ->
            Timber.i("Applying Arctic skin: $skin")
            val localSkin = mapToLocalSkin(skin)
            ArcticTheme.activeSkin = localSkin
        } ?: run {
            Timber.d("Resetting to default Arctic theme")
            ArcticTheme.activeSkin = null
        }
    }

    private fun mapToLocalSkin(skinId: String): Skin {
        return when (skinId) {
            "arctic_fuse" -> Skin.ARCTIC_FUSE
            "kyuubi" -> Skin.DEEP_PURPLE
            "shadow" -> Skin.STARRY_NIGHT
            "neon" -> Skin.CHERRY_BLOSSOM
            "amoled_black" -> Skin.AMOLED_BLACK
            "ocean_blue" -> Skin.OCEAN_BLUE
            "forest_green" -> Skin.FOREST_GREEN
            else -> Skin.ARCTIC_FUSE
        }
    }

    interface KuroThemeEntryPoint {
        fun syncRepository(): KuroSyncRepository
    }
}

/**
 * Runtime Arctic Fuse theme configuration.
 * Uses Material 3 dynamic theming with skin overrides.
 */
object ArcticTheme {

    private var _activeSkin: Skin? = null
    var activeSkin: Skin?
        get() = _activeSkin
        set(value) {
            _activeSkin = value
            // In a real implementation, this would update the MaterialTheme colors
            // via composition local or a custom theme wrapper
        }

    // Predefined Arctic Fuse skins mapping to local Skin enum
    private val skins = mapOf(
        "arctic_fuse" to Skin.ARCTIC_FUSE,
        "kyuubi" to Skin.DEEP_PURPLE,
        "shadow" to Skin.STARRY_NIGHT,
        "neon" to Skin.CHERRY_BLOSSOM,
        "amoled_black" to Skin.AMOLED_BLACK,
        "ocean_blue" to Skin.OCEAN_BLUE,
        "forest_green" to Skin.FOREST_GREEN,
    )

    fun getSkin(skinId: String?): Skin? {
        return skinId?.let { skins[it] }
    }

    fun getAllSkins(): List<String> = skins.keys.toList()
}