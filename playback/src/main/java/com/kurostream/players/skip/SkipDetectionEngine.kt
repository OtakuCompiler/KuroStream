package com.kurostream.players.skip

import android.content.Context
import com.kurostream.data.skip.AniSkipClient
import com.kurostream.data.skip.IntroDbClient
import com.kurostream.data.skip.SkipInterval
import com.kurostream.data.skip.SkipType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Coordinates AniSkip + IntroDB skip-time resolution and exposes
 * reactive [skipIntro] / [skipOutro] signals to the player overlay.
 *
 * Resolution strategy
 * ───────────────────
 * 1. Check SharedPreferences cache first (instant, no network).
 * 2. Fetch AniSkip + IntroDB **in parallel** (both fire at the same time).
 * 3. Merge results: AniSkip wins on INTRO/OUTRO when present; IntroDB fills
 *    gaps (some titles only have one or the other).
 * 4. Persist merged result to SharedPreferences for future sessions.
 *
 * Call [loadAndFetch] at episode start. Call [checkPosition] on every
 * player position update; the emitted StateFlow values drive the
 * "Skip Intro / Skip Outro" overlay buttons.
 */
class SkipDetectionEngine(
    private val context: Context,
    private val aniSkipClient: AniSkipClient,
    private val introDbClient: IntroDbClient,
) {

    private val prefs = context.getSharedPreferences("skip_markers_v2", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _skipIntro    = MutableStateFlow(false)
    private val _skipOutro    = MutableStateFlow(false)
    private val _isLoading    = MutableStateFlow(false)

    val skipIntro: StateFlow<Boolean> = _skipIntro.asStateFlow()
    val skipOutro: StateFlow<Boolean> = _skipOutro.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Currently active markers for position checking. */
    private var activeMarkers: SkipMarkers? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Load cached markers and, if [malId] or [anilistId] is non-null,
     * refresh them in the background from the network APIs.
     *
     * @param mediaId       Unique key used for local caching (e.g. "mal_12345_ep3")
     * @param malId         MAL ID for AniSkip lookups (null = skip AniSkip)
     * @param anilistId     AniList ID for IntroDB lookups (null = skip IntroDB)
     * @param episodeNumber Episode number (1-based)
     * @param episodeLengthMinutes Approximate episode length for AniSkip boundary hints
     */
    fun loadAndFetch(
        mediaId: String,
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int,
        episodeLengthMinutes: Double = 24.0,
    ) {
        // Immediately apply cached markers so the button shows on replay.
        activeMarkers = loadCached(mediaId)

        // Kick off network fetch even if we have a cache hit — data may have improved.
        scope.launch {
            _isLoading.value = true
            try {
                val merged = fetchAndMerge(
                    mediaId, malId, anilistId, episodeNumber, episodeLengthMinutes
                )
                if (merged != null) {
                    activeMarkers = merged
                    saveToCache(mediaId, merged)
                    Timber.d("SkipDetection[$mediaId]: loaded intro=${merged.introStartMs}-${merged.introEndMs}ms outro=${merged.outroStartMs}-${merged.outroEndMs}ms")
                } else {
                    Timber.d("SkipDetection[$mediaId]: no skip data from either API")
                }
            } catch (e: Exception) {
                Timber.w(e, "SkipDetection[$mediaId]: fetch failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Call on each player position update (ms). Updates [skipIntro]/[skipOutro]. */
    fun checkPosition(positionMs: Long) {
        val m = activeMarkers ?: return
        _skipIntro.value = m.hasIntro && positionMs in m.introStartMs..m.introEndMs
        _skipOutro.value = m.hasOutro && positionMs in m.outroStartMs..m.outroEndMs
    }

    /** Reset state when moving to a new episode. */
    fun reset() {
        activeMarkers    = null
        _skipIntro.value = false
        _skipOutro.value = false
        _isLoading.value = false
    }

    // ── Network resolution ────────────────────────────────────────────────────

    private suspend fun fetchAndMerge(
        mediaId: String,
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int,
        episodeLengthMinutes: Double,
    ): SkipMarkers? = coroutineScope {
        val aniSkipDeferred = if (malId != null) {
            async(Dispatchers.IO) {
                aniSkipClient.getSkipTimes(malId, episodeNumber, episodeLengthMinutes)
                    .getOrNull()
            }
        } else null

        val introDbDeferred = if (anilistId != null) {
            async(Dispatchers.IO) {
                introDbClient.getSkipTimes(anilistId, episodeNumber)
                    .getOrNull()
            }
        } else null

        val aniSkipResult = aniSkipDeferred?.await()
        val introDbResult = introDbDeferred?.await()

        // Merge: AniSkip takes priority; IntroDB fills missing segments.
        val combined = mutableMapOf<SkipType, SkipInterval>()
        introDbResult?.forEach { (type, iv) -> combined[type] = iv }
        aniSkipResult?.forEach { (type, iv) -> combined[type] = iv } // overwrite

        if (combined.isEmpty()) return@coroutineScope null

        val intro = combined[SkipType.INTRO] ?: combined[SkipType.MIXED_INTRO]
        val outro = combined[SkipType.OUTRO] ?: combined[SkipType.MIXED_OUTRO]
        val recap = combined[SkipType.RECAP]

        SkipMarkers(
            introStartMs = intro?.let { (it.startTime * 1000).toLong() } ?: -1L,
            introEndMs   = intro?.let { (it.endTime   * 1000).toLong() } ?: -1L,
            outroStartMs = (outro ?: recap)?.let { (it.startTime * 1000).toLong() } ?: -1L,
            outroEndMs   = (outro ?: recap)?.let { (it.endTime   * 1000).toLong() } ?: -1L,
        )
    }

    // ── SharedPreferences cache ───────────────────────────────────────────────

    private fun loadCached(mediaId: String): SkipMarkers? {
        val introStart = prefs.getLong("${mediaId}_is", -1L)
        val introEnd   = prefs.getLong("${mediaId}_ie", -1L)
        return if (introStart >= 0 && introEnd > introStart) {
            SkipMarkers(
                introStartMs = introStart,
                introEndMs   = introEnd,
                outroStartMs = prefs.getLong("${mediaId}_os", -1L),
                outroEndMs   = prefs.getLong("${mediaId}_oe", -1L),
            )
        } else null
    }

    private fun saveToCache(mediaId: String, markers: SkipMarkers) {
        prefs.edit()
            .putLong("${mediaId}_is", markers.introStartMs)
            .putLong("${mediaId}_ie", markers.introEndMs)
            .putLong("${mediaId}_os", markers.outroStartMs)
            .putLong("${mediaId}_oe", markers.outroEndMs)
            .apply()
    }

    // ── Data types ────────────────────────────────────────────────────────────

    data class SkipMarkers(
        val introStartMs: Long,
        val introEndMs:   Long,
        val outroStartMs: Long,
        val outroEndMs:   Long,
    ) {
        val hasIntro: Boolean get() = introStartMs >= 0 && introEndMs > introStartMs
        val hasOutro: Boolean get() = outroStartMs >= 0 && outroEndMs > outroStartMs
    }
}
