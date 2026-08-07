/*
 * PlatformMemoryOptimizer — runtime watchdog that enforces the
 * `PlatformProfile.ramBudgetMb` cap and triggers aggressive buffer purges
 * when memory pressure rises. This is what keeps webOS 4/5/6 from OOM
 * killing the app while still delivering 4K + Atmos.
 *
 * Three layers:
 *   1. Proactive caps: every cache has a size limit from the profile.
 *   2. Soft pressure: when usage > ramTriggerPurgeMb, drop prefetch cache.
 *   3. Hard pressure: when usage > ramBudgetMb, drop playback buffer too
 *      (player will rebuffer from network).
 *
 * The optimizer is a thin coordinator; the actual caches it manages are
 * per-platform implementations (Android uses LruCache, webOS uses a custom
 * module-aware allocator since ARTGC on webOS is unusual, etc.).
 */
package com.kurostream.domain.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BoundedCache {
    val name: String
    fun currentBytes(): Long
    fun trimTo(targetBytes: Long)
    fun clear()
}

class PlatformMemoryOptimizer(
    val profile: PlatformProfile,
    private val runtimeMemoryProbe: () -> Long,
) {
    private val caches = mutableListOf<BoundedCache>()
    private val _state = MutableStateFlow(State.NOMINAL)
    val state: StateFlow<State> = _state.asStateFlow()

    fun register(cache: BoundedCache) {
        caches += cache
    }

    fun tick(): Action {
        val runtimeBytes = runtimeMemoryProbe()
        val cacheBytes = caches.sumOf { it.currentBytes() }
        val totalMb = (runtimeBytes / (1024 * 1024)).toInt()

        return when {
            totalMb >= profile.ramBudgetMb -> {
                caches.forEach { it.trimTo(profile.videoFrameCacheBytes / 4) }
                emit(Action.HARD_TRIM, totalMb)
                Action.HARD_TRIM
            }
            totalMb >= profile.ramTriggerPurgeMb -> {
                caches.forEach { it.trimTo(profile.videoFrameCacheBytes / 2) }
                emit(Action.SOFT_TRIM, totalMb)
                Action.SOFT_TRIM
            }
            else -> {
                emit(Action.NOMINAL, totalMb)
                Action.NOMINAL
            }
        }
    }

    private fun emit(a: Action, mb: Int) {
        _state.value = when (a) {
            Action.NOMINAL -> State.NOMINAL
            Action.SOFT_TRIM -> State.SOFT_TRIM
            Action.HARD_TRIM -> State.HARD_TRIM
        }
    }

    enum class State { NOMINAL, SOFT_TRIM, HARD_TRIM }
    enum class Action { NOMINAL, SOFT_TRIM, HARD_TRIM }
}
