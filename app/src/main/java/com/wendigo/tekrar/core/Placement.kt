package com.wendigo.tekrar.core

data class PlacementResult(
    val band: String,
    val recommendedNewPerDay: Int,
)

/**
 * Kaba seviye yerleştirmesi. Kesin bir FSRS/AI tahmini değildir; yalnızca testte
 * "biliyorum" denilen kelime oranına göre bir başlangıç bandı ve günlük yeni kelime
 * önerisi üretir.
 *
 * Sınırlar: %40'ın altı -> 20, %40–%70 (dahil) -> 15, %70'in üstü -> 10.
 */
object Placement {

    fun score(known: Int, total: Int): PlacementResult {
        val safeTotal = total.coerceAtLeast(0)
        val safeKnown = known.coerceIn(0, safeTotal)
        if (safeTotal == 0) {
            return PlacementResult(band = "Belirsiz", recommendedNewPerDay = 15)
        }
        val percent = safeKnown * 100 / safeTotal
        return when {
            percent < 40 -> PlacementResult(
                band = "A1–A2",
                recommendedNewPerDay = 20,
            )
            percent <= 70 -> PlacementResult(
                band = "B1",
                recommendedNewPerDay = 15,
            )
            else -> PlacementResult(
                band = "B2–C1",
                recommendedNewPerDay = 10,
            )
        }
    }
}
