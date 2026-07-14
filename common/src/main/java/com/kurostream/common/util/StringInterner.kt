package com.kurostream.common.util

object StringInterner {
    fun intern(str: String?): String? {
        return str?.intern()
    }

    fun internAll(strings: Iterable<String>): List<String> {
        return strings.map { it.intern() }
    }

    fun internTitle(providerId: String, title: String): String {
        return "$providerId:$title".intern()
    }

    fun internMetadata(name: String): String {
        return name.intern()
    }

    fun getStats(): InternerStats = InternerStats(0, 0, 0, 0, 0.0)

    fun clear() = Unit
    fun shrink() = Unit

    fun preloadCommonStrings() {
        listOf(
            "kitsu", "anilist", "mal", "tmdb", "tvdb", "imdb",
            "Action", "Adventure", "Comedy", "Drama",
            "TV", "Movie", "OVA", "ONA", "Special", "Music"
        ).forEach { it.intern() }
    }

    data class InternerStats(
        val totalEntries: Int,
        val hits: Long,
        val misses: Long,
        val evicted: Long,
        val hitRate: Double
    ) {
        override fun toString(): String = "InternerStats(entries=$totalEntries)"
    }
}

fun String?.interned(): String? = StringInterner.intern(this)
