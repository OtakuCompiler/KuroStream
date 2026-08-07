package com.kurostream.desktop.data

import androidx.compose.runtime.Stable
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Desktop SQLite cache. Uses the bundled SQLite JDBC driver so the app
 * doesn't require a system SQLite version. Mirrors the role of Room on
 * Android but without code-gen overhead — queries are written directly.
 *
 * Schema is intentionally minimal: only persistent state the desktop app
 * needs on top of the cloud-synced data (search history, recently played,
 * cached posters metadata, offline episode metadata).
 */
@Stable
class DesktopCache private constructor(private val conn: Connection) {

    fun close() {
        runCatching { conn.close() }
    }

    companion object {
        fun open(file: File): DesktopCache {
            Class.forName("org.sqlite.JDBC")
            val conn = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS search_history (
                        query TEXT PRIMARY KEY,
                        last_used INTEGER NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS recently_played (
                        media_id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        backdrop_url TEXT,
                        last_position_ms INTEGER NOT NULL,
                        last_played INTEGER NOT NULL
                    );
                    CREATE TABLE IF NOT EXISTS cached_posters (
                        media_id TEXT PRIMARY KEY,
                        url TEXT NOT NULL,
                        local_path TEXT,
                        last_updated INTEGER NOT NULL
                    );
                    """.trimIndent()
                )
            }
            return DesktopCache(conn)
        }
    }
}
