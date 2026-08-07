/*
 * AdaptiveTranscoder — on-the-fly stream transcoding for codecs the host
 * can't decode natively. This is the concrete fix for the webOS / Tizen
 * "error occurred when decoding" / "video cannot be played" bugs.
 *
 * The transcoder is a thin abstraction; the heavy lifting lives in the
 * per-platform implementation:
 *   - Android / Android TV / Fire TV: FFmpegKit (libVLC + FFmpeg)
 *   - Linux / Windows desktop: bundled FFmpeg via ProcessBuilder
 *   - webOS / Tizen: FFmpeg.wasm in the WebWorker thread (Tizen/webOS
 *     both run WebKit so they can host WASM)
 *
 * Strategy: when the user picks a stream the resolver runs
 * `CodecCompatibilityMatrix.check()` first. If the verdict requires a
 * transcode, the playback service starts an AdaptiveTranscoder session
 * pointing at the original torrent source, and feeds the transcoded
 * output (H.264 + AAC) into the native player.
 */
package com.kurostream.domain.platform

import kotlinx.coroutines.flow.Flow

data class TranscodeSessionConfig(
    val source: TranscodeSource,
    val requirements: TranscodeRequirements,
    val profile: PlatformProfile,
)

sealed class TranscodeSource {
    data class HttpUrl(val url: String) : TranscodeSource()
    data class Magnet(val magnet: String, val infoHash: String) : TranscodeSource()
    data class LocalFile(val path: String) : TranscodeSource()
    data class TorrentHandle(val torrentId: String) : TranscodeSource()
}

sealed class TranscodeEvent {
    data object Starting : TranscodeEvent()
    data class Progress(val percent: Int, val outputBitrateKbps: Int) : TranscodeEvent()
    data class Ready(val outputUrl: String, val mimeType: String) : TranscodeEvent()
    data class Error(val message: String) : TranscodeEvent()
    data object Finished : TranscodeEvent()
}

interface AdaptiveTranscoder {
    val isAvailable: Boolean

    fun start(config: TranscodeSessionConfig): Flow<TranscodeEvent>

    fun cancel(sessionId: String)
}
