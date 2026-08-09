// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.security

/**
 * InputSanitizer — defence in depth for user-provided strings that flow into
 * file paths, URLs, or shell arguments. Used by add-on installer, search
 * box, deep-link parser, and torrent query box.
 */
object InputSanitizer {

    private val ALLOWED_URL_SCHEMES = setOf("http", "https")

    /**
     * Sanitize a user-supplied URL. Returns null if unsafe.
     * Allows only http/https schemes and rejects control characters.
     */
    fun sanitizeUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim().take(2048)
        if (trimmed.any { it.code < 0x20 || it.code == 0x7F }) return null
        val lowered = trimmed.lowercase()
        if (!ALLOWED_URL_SCHEMES.any { lowered.startsWith("$it://") }) return null
        return trimmed
    }

    /**
     * Sanitize a free-text search query. Strips control chars, caps length,
     * rejects path-traversal tokens.
     */
    fun sanitizeQuery(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = raw.filter { ch ->
            val ok = ch.isLetterOrDigit() || ch.isWhitespace() ||
                ch in setOf('-', '_', '.', '\'', ':', '!', '?')
            ok && ch.code >= 0x20 && ch.code != 0x7F
        }
        return cleaned.take(120)
    }

    /**
     * Sanitize a file path segment. Rejects "..", null bytes, control chars,
     * absolute paths.
     */
    fun sanitizePathSegment(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.contains("..") || raw.startsWith("/") || raw.startsWith("\\")) return null
        if (raw.contains('\u0000')) return null
        if (raw.any { it.code < 0x20 || it.code == 0x7F }) return null
        return raw.take(255)
    }

    /**
     * Validate a media id (e.g. IMDB id like tt1234567, or numeric Kitsu id).
     * Returns true if it looks safe to use as a parameter.
     */
    fun isValidMediaId(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        if (id.length > 32) return false
        return id.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    /**
     * Sanitize a free-form overview/synopsis text for safe rendering.
     * Strips HTML, control characters, and excessive whitespace.
     * Result is plain text suitable for display.
     */
    fun sanitizeOverview(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.length > 4_000) return null
        val noHtml = raw
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&", "&")
            .replace("<", "<")
            .replace(">", ">")
            .replace("\"", "\"")
            .replace("'", "'")
        val cleaned = noHtml.filter { ch -> ch.code >= 0x20 && ch.code != 0x7F }
        val collapsed = cleaned.replace(Regex("\\s+"), " ").trim()
        return collapsed.take(1_500).ifBlank { null }
    }
}
