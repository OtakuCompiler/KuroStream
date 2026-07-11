// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.common.util

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * String interner for deduplicating frequently repeated strings
 * like media titles, provider IDs, genre names, studio names.
 * 
 * Uses WeakHashMap semantics to allow GC of unused interned strings.
 */
object StringInterner {
    
    private val TAG = "StringInterner"
    
    // Main interning map - using ConcurrentHashMap for thread safety
    // Key: original string, Value: WeakReference to interned string
    private val interned = ConcurrentHashMap<String, WeakReference<String>>()
    
    // Stats
    private var hits = 0L
    private var misses = 0L
    private var evicted = 0L
    
    
    /**
     * Intern a string - returns canonical instance.
     * If string already interned, returns existing instance.
     */
    @Suppress("UNUSED_PARAMETER")
    fun intern(str: String?): String? {
        if (str == null || str.isEmpty()) return str
        
        val existing = interned[str]?.get()
        if (existing != null) {
            hits++
            return existing
        }
        
        misses++
        val internedStr = str.intern()
        interned[str] = WeakReference(internedStr)
        return internedStr
    }
    
    /**
     * Intern multiple strings at once.
     */
    fun internAll(strings: Iterable<String>): List<String> {
        return strings.map { intern(it) ?: "" }
    }
    
    /**
     * Intern a media title with provider prefix for better deduplication.
     */
    fun internTitle(providerId: String, title: String): String {
        return intern("$providerId:$title") ?: title
    }
    
    /**
     * Intern a genre/studio name.
     */
    fun internMetadata(name: String): String {
        return intern(name) ?: name
    }
    
    /**
     * Get current stats.
     */
    fun getStats(): InternerStats {
        synchronized(this) {
            cleanupStaleEntries()
        }
        return InternerStats(
            totalEntries = interned.size,
            hits = hits,
            misses = misses,
            evicted = evicted,
            hitRate = if (hits + misses > 0) hits.toDouble() / (hits + misses) else 0.0
        )
    }
    
    /**
     * Clear all interned strings (for memory pressure).
     */
    fun clear() {
        synchronized(this) {
            interned.clear()
            hits = 0
            misses = 0
            evicted = 0
        }
    }
    
    /**
     * Shrink the interner by removing stale entries.
     */
    fun shrink() {
        synchronized(this) {
            cleanupStaleEntries()
        }
    }
    
    private fun cleanupStaleEntries() {
        var removed = 0
        interned.entries.removeIf { entry ->
            val ref = entry.value
            if (ref.get() == null) {
                removed++
                true
            } else {
                false
            }
        }
        if (removed > 0) {
            evicted += removed
        }
    }
    
    /**
     * Pre-intern common strings at startup.
     */
    fun preloadCommonStrings() {
        // Common providers
        listOf("kitsu", "anilist", "mal", "tmdb", "tvdb", "imdb", "stremio", "torrserver", "jellyfin", "emby", "plex")
            .forEach { intern(it) }
        
        // Common genres
        listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery", 
               "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller")
            .forEach { intern(it) }
        
        // Common studios
        listOf("MAPPA", "Ufotable", "Wit Studio", "Bones", "Kyoto Animation", "Madhouse",
               "Production I.G", "Studio Trigger", "CloverWorks", "A-1 Pictures", "J.C.Staff")
            .forEach { intern(it) }
        
        // Common statuses
        listOf("Airing", "Finished", "Not Yet Aired", "Cancelled", "On Hiatus")
            .forEach { intern(it) }
        
        // Common media types
        listOf("TV", "Movie", "OVA", "ONA", "Special", "Music")
            .forEach { intern(it) }
    }
    
    data class InternerStats(
        val totalEntries: Int,
        val hits: Long,
        val misses: Long,
        val evicted: Long,
        val hitRate: Double
    ) {
        override fun toString(): String {
            return "InternerStats(entries=$totalEntries, hits=$hits, misses=$misses, " +
                   "evicted=$evicted, hitRate=${(hitRate * 100).toInt()}%)"
        }
    }
}

/**
 * Extension for easy interning.
 */
fun String?.interned(): String? = StringInterner.intern(this)