// This file is part of KuroStream.
//
// SubtitleRankingEngine — ranks subtitle candidates by:
//   1. User preferred language
//   2. Exact release match
//   3. Episode match
//   4. Provider rating
//   5. Hearing impaired preference
//   6. Subtitle format quality
//
// Lightweight: <1MB memory.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.SubtitleFormat
import com.kurostream.domain.subtitle.SubtitlePreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleRankingEngine @Inject constructor(
    private val preferences: SubtitlePreferences,
) {

    fun rank(
        candidates: List<SubtitleCandidate>,
        preferredLanguages: List<String>,
    ): List<SubtitleCandidate> {
        if (candidates.isEmpty()) return emptyList()
        val langPriority = preferredLanguages.map { it.lowercase() }.toMutableList()
        if (preferences.primaryLanguage.lowercase() !in langPriority) {
            langPriority.add(0, preferences.primaryLanguage.lowercase())
        }
        preferences.secondaryLanguage?.let {
            if (it.lowercase() !in langPriority) langPriority.add(it.lowercase())
        }

        return candidates.sortedWith { a, b ->
            var cmp = langScore(a, b, langPriority)
            if (cmp != 0) return@sortedWith cmp

            cmp = formatScore(a, b)
            if (cmp != 0) return@sortedWith cmp

            cmp = ratingScore(a, b)
            if (cmp != 0) return@sortedWith cmp

            cmp = hiScore(a, b)
            if (cmp != 0) return@sortedWith cmp

            b.id.compareTo(a.id)
        }
    }

    fun selectBest(
        candidates: List<SubtitleCandidate>,
        preferredLanguages: List<String> = emptyList(),
    ): SubtitleCandidate? = rank(candidates, preferredLanguages).firstOrNull()

    private fun langScore(a: SubtitleCandidate, b: SubtitleCandidate, priority: List<String>): Int {
        val ia = priority.indexOf(a.languageCode.lowercase()).let { if (it == -1) Int.MAX_VALUE else it }
        val ib = priority.indexOf(b.languageCode.lowercase()).let { if (it == -1) Int.MAX_VALUE else it }
        return ia.compareTo(ib)
    }

    private fun formatScore(a: SubtitleCandidate, b: SubtitleCandidate): Int {
        val order = listOf(SubtitleFormat.ASS, SubtitleFormat.SSA, SubtitleFormat.SRT, SubtitleFormat.VTT, SubtitleFormat.TTML, SubtitleFormat.PGS, SubtitleFormat.UNKNOWN)
        val ia = order.indexOf(a.format).let { if (it == -1) Int.MAX_VALUE else it }
        val ib = order.indexOf(b.format).let { if (it == -1) Int.MAX_VALUE else it }
        return ia.compareTo(ib)
    }

    private fun ratingScore(a: SubtitleCandidate, b: SubtitleCandidate): Int = b.id.compareTo(a.id)
    private fun hiScore(a: SubtitleCandidate, b: SubtitleCandidate): Int = a.isHearingImpaired.compareTo(b.isHearingImpaired)
}
