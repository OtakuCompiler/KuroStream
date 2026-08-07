/*
 * PlatformAwareResolver — wraps any `StreamResolver` with the codec
 * compatibility matrix + memory profile so that:
 *
 *   - Streams the host can't play at all (no transcode possible) are
 *     filtered out before they reach the UI.
 *   - Streams that need transcode are flagged with a
 *     `TranscodeRequirements` payload the playback layer uses to spin up
 *     the `AdaptiveTranscoder`.
 *
 * This is the entry point that fixes the LG webOS / Tizen 4K P2P playback
 * crash — instead of letting the player try and fail, we decide up-front
 * whether each stream will work and only surface working ones.
 */
package com.kurostream.domain.resolver

import com.kurostream.domain.platform.CodecCompatibilityMatrix
import com.kurostream.domain.platform.PlatformProfile
import com.kurostream.domain.platform.StreamProbe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ResolvedStream(
    val source: StreamSource,
    val probe: StreamProbe,
    val verdict: com.kurostream.domain.platform.CompatibilityVerdict,
)

class PlatformAwareResolver(
    private val inner: StreamResolver,
    private val profile: PlatformProfile,
) {
    fun resolve(query: String): Flow<List<ResolvedStream>> = flow {
        val raw = inner.resolve(query)
        val probed = raw.map { source ->
            val probe = probeStream(source)
            val verdict = CodecCompatibilityMatrix.check(profile, probe)
            ResolvedStream(source, probe, verdict)
        }
        // Filter out streams we can't play OR transcode.
        val filtered = probed.filter { it.verdict.canPlay }
        // Prefer direct-play over transcode when both are available.
        val sorted = filtered.sortedByDescending { it.verdict.canPlayDirectly }
        emit(sorted)
    }

    /**
     * Probe a stream's codecs. In production this calls the metadata
     * probe service (mediainfo or ffprobe over HTTP); the stub here
     * just parses the source's metadata fields.
     */
    private fun probeStream(source: StreamSource): StreamProbe {
        val meta = source.metadata
        return StreamProbe(
            videoCodec = meta.videoCodec ?: "hevc",
            videoProfile = meta.videoProfile,
            videoBitDepth = meta.videoBitDepth ?: 8,
            width = meta.width ?: 1920,
            height = meta.height ?: 1080,
            frameRate = meta.frameRate ?: 24.0,
            hdrType = meta.hdrType ?: com.kurostream.domain.platform.HdrType.NONE,
            audioCodec = meta.audioCodec ?: "aac",
            audioChannels = meta.audioChannels ?: 2,
            audioSampleRateHz = meta.audioSampleRateHz ?: 48_000,
            container = meta.container ?: "mkv",
            hasMultipleAudioTracks = meta.audioTrackCount > 1,
            hasEmbeddedSubtitles = meta.subtitleTrackCount > 0,
        )
    }
}
