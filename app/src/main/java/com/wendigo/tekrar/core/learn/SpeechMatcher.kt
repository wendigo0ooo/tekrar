package com.wendigo.tekrar.core.learn

import kotlin.math.roundToInt

/**
 * STT metni ile hedef cümle arasında 0-100 **kelime benzerliği**.
 *
 * Kelime dizileri üzerinde Levenshtein mesafesi kullanılır:
 * `100 * (1 - mesafe / max(hedef, söylenen))`. Bu bir fonem/telaffuz
 * ölçümü değildir; arayüzde "benzerlik" olarak sunulmalıdır.
 */
object SpeechMatcher {

    const val PASS_SCORE = 70

    fun score(target: String, transcript: String): Int {
        val expected = AnswerNormalizer.words(target)
        val spoken = AnswerNormalizer.words(transcript)
        if (expected.isEmpty() && spoken.isEmpty()) return 100
        if (expected.isEmpty() || spoken.isEmpty()) return 0
        val distance = wordDistance(expected, spoken)
        val longest = maxOf(expected.size, spoken.size)
        val similarity = 100.0 * (1.0 - distance.toDouble() / longest)
        return similarity.roundToInt().coerceIn(0, 100)
    }

    private fun wordDistance(first: List<String>, second: List<String>): Int {
        if (first.isEmpty()) return second.size
        if (second.isEmpty()) return first.size
        var previous = IntArray(second.size + 1) { it }
        var current = IntArray(second.size + 1)
        for (i in 1..first.size) {
            current[0] = i
            for (j in 1..second.size) {
                val substitution = previous[j - 1] + if (first[i - 1] == second[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    substitution,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[second.size]
    }
}
