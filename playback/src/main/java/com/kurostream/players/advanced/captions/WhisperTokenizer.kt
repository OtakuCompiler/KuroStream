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

package com.kurostream.players.advanced.captions

/**
 * Whisper tokenizer for decoding model output tokens.
 * Simplified implementation - production would use a full BPE tokenizer.
 */
class WhisperTokenizer {

    // Special tokens
    companion object {
        const val SOT = 50258  // Start of transcript
        const val EOT = 50257  // End of transcript
        const val BLANK = 220
        const val TRANSCRIBE = 50359
        const val TRANSLATE = 50358
        const val NO_TIMESTAMPS = 50363

        // Language token offsets
        private val languageTokens = mapOf(
            "en" to 50259,
            "zh" to 50260,
            "de" to 50261,
            "es" to 50262,
            "ru" to 50263,
            "ko" to 50264,
            "fr" to 50265,
            "ja" to 50266,
            "pt" to 50267,
            "tr" to 50268,
            "pl" to 50269,
            "ca" to 50270,
            "nl" to 50271,
            "ar" to 50272,
            "sv" to 50273,
            "it" to 50274,
            "id" to 50275,
            "hi" to 50276,
            "fi" to 50277,
            "vi" to 50278,
            "he" to 50279,
            "uk" to 50280,
            "el" to 50281,
            "ms" to 50282,
            "cs" to 50283,
            "ro" to 50284,
            "da" to 50285,
            "hu" to 50286,
            "ta" to 50287,
            "no" to 50288,
            "th" to 50289,
            "ur" to 50290,
            "hr" to 50291,
            "bg" to 50292,
            "lt" to 50293,
            "la" to 50294,
            "mi" to 50295,
            "ml" to 50296,
            "cy" to 50297,
            "sk" to 50298,
            "te" to 50299,
            "fa" to 50300,
            "lv" to 50301,
            "bn" to 50302,
            "sr" to 50303,
            "az" to 50304,
            "sl" to 50305,
            "kn" to 50306,
            "et" to 50307,
            "mk" to 50308,
            "br" to 50309,
            "eu" to 50310,
            "is" to 50311,
            "hy" to 50312,
            "ne" to 50313,
            "mn" to 50314,
            "bs" to 50315,
            "kk" to 50316,
            "sq" to 50317,
            "sw" to 50318,
            "gl" to 50319,
            "mr" to 50320,
            "pa" to 50321,
            "si" to 50322,
            "km" to 50323,
            "sn" to 50324,
            "yo" to 50325,
            "so" to 50326,
            "af" to 50327,
            "oc" to 50328,
            "ka" to 50329,
            "be" to 50330,
            "tg" to 50331,
            "sd" to 50332,
            "gu" to 50333,
            "am" to 50334,
            "yi" to 50335,
            "lo" to 50336,
            "uz" to 50337,
            "fo" to 50338,
            "ht" to 50339,
            "ps" to 50340,
            "tk" to 50341,
            "nn" to 50342,
            "mt" to 50343,
            "sa" to 50344,
            "lb" to 50345,
            "my" to 50346,
            "bo" to 50347,
            "tl" to 50348,
            "mg" to 50349,
            "as" to 50350,
            "tt" to 50351,
            "haw" to 50352,
            "ln" to 50353,
            "ha" to 50354,
            "ba" to 50355,
            "jw" to 50356,
            "su" to 50357,
        )
    }

    fun getLanguageToken(languageCode: String): Int {
        return languageTokens[languageCode] ?: languageTokens["en"]!!
    }

    /**
     * Decode token IDs to text.
     * Simplified - production would use a proper BPE decoder.
     */
    fun decode(tokens: IntArray): String {
        val sb = StringBuilder()
        for (token in tokens) {
            when {
                token == SOT || token == EOT || token == TRANSCRIBE ||
                token == TRANSLATE || token == NO_TIMESTAMPS -> continue
                token in languageTokens.values -> continue
                token == BLANK -> sb.append(" ")
                token < 256 -> sb.append(token.toChar())
                else -> {
                    // Simplified byte fallback for unknown tokens
                    sb.append("?")
                }
            }
        }
        return sb.toString().trim()
    }
}
