package com.wendigo.tekrar.core

import java.util.Locale
import kotlin.math.roundToInt

data class SessionHardWord(
    val term: String,
    val failures: Int,
)

object SessionSummary {

    fun accuracyPercent(correct: Int, reviewed: Int): Int {
        if (reviewed <= 0) return 0
        val safeCorrect = correct.coerceIn(0, reviewed)
        return (safeCorrect.toDouble() * 100.0 / reviewed).roundToInt()
    }

    fun hardestWords(
        failures: Map<String, Int>,
        limit: Int = 3,
    ): List<SessionHardWord> =
        failures.asSequence()
            .filter { it.key.isNotBlank() && it.value > 0 }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key.lowercase(Locale.ROOT) }
            )
            .take(limit.coerceAtLeast(0))
            .map { SessionHardWord(it.key, it.value) }
            .toList()
}
