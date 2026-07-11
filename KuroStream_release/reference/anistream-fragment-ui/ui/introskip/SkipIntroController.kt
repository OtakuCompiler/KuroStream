package com.kurostream.legacyui.anistream.ui.introskip

import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import com.kurostream.data.anistream.introskip.AniSkipApi
import com.kurostream.data.anistream.introskip.SkipResult
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkipIntroController @Inject constructor(
    private val aniSkipApi: AniSkipApi
) {

    private var currentSkips: List<SkipResult> = emptyList()
    private var skipJob: Job? = null
    private var skipButton: Button? = null
    private var onSkipRequested: ((Double) -> Unit)? = null

    suspend fun loadSkipTimes(
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int
    ) {
        val result = aniSkipApi.getSkipTimes(malId, anilistId, episodeNumber)
        result.onSuccess { response ->
            currentSkips = response.results
        }
    }

    fun attachToPlayer(
        skipButton: Button,
        getCurrentPositionMs: () -> Long,
        seekTo: (Long) -> Unit
    ) {
        this.skipButton = skipButton
        this.onSkipRequested = { positionMs ->
            seekTo(positionMs.toLong() * 1000)
        }

        skipJob?.cancel()
        skipJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                checkAndShowSkipButton(getCurrentPositionMs() / 1000.0)
                delay(500)
            }
        }
    }

    private fun checkAndShowSkipButton(currentPositionSec: Double) {
        val activeSkip = currentSkips.find { skip ->
            currentPositionSec >= skip.interval.startTime &&
            currentPositionSec <= skip.interval.endTime - 1.0
        }

        skipButton?.apply {
            if (activeSkip != null) {
                text = when (activeSkip.skipType) {
                    "op" -> "Skip Intro"
                    "ed" -> "Skip Outro"
                    "recap" -> "Skip Recap"
                    else -> "Skip"
                }
                isVisible = true
                setOnClickListener {
                    onSkipRequested?.invoke(activeSkip.interval.endTime)
                    isVisible = false
                }
            } else {
                isVisible = false
            }
        }
    }

    fun detach() {
        skipJob?.cancel()
        skipJob = null
        skipButton = null
        currentSkips = emptyList()
    }
}
