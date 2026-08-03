// This file is part of KuroStream.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.players.selector

/**
 * Playback engine backends available to the selector.
 */
enum class PlayerBackend {
    AUTO,
    MEDIA3,
    VLC,
    MPV,
    TORRENT,
}
