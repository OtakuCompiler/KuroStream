package com.kurostream.players.selector

/**
 * Playback backend selector.
 *
 * CURRENT REALITY: KuroStream currently ships a single Media3/ExoPlayer backend.
 * The values below reflect the original aspirational multi-engine design.
 * Only [MEDIA3] has a production implementation at this time.
 *
 * - MPV / VLC / TORRENT: not implemented. Do not reference these from production
 *   code unless a corresponding engine wrapper is also restored.
 */
enum class PlayerBackend { MPV, VLC, MEDIA3, AUTO, TORRENT }
