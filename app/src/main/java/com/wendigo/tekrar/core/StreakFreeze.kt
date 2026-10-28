package com.wendigo.tekrar.core

import java.time.LocalDate

data class FreezeGrant(
    val granted: Int,
    val milestone: Int,
)

data class FreezeDecision(
    val days: List<LocalDate>,
    val stockAfter: Int,
) {
    val consumed: Int get() = days.size
}

/**
 * Seri dondurma kararları. Saf fonksiyonlar; kalıcılık ve saat dilimi çağırana aittir.
 */
object StreakFreeze {

    const val MAX_FREEZES = 2
    const val MILESTONE = 7

    private const val MAX_LOOKBACK = 400

    /**
     * Seri 7'nin katına ulaştığında en fazla bir dondurma kazandırır.
     * [lastMilestone] aynı eşiğin tekrar ödüllendirilmesini engeller.
     */
    fun grant(streak: Int, lastMilestone: Int, stock: Int): FreezeGrant {
        val safeStreak = streak.coerceAtLeast(0)
        val reached = (safeStreak / MILESTONE) * MILESTONE
        if (reached <= 0 || reached <= lastMilestone) {
            return FreezeGrant(0, maxOf(lastMilestone, reached))
        }
        val safeStock = stock.coerceIn(0, MAX_FREEZES)
        val granted = if (safeStock < MAX_FREEZES) 1 else 0
        return FreezeGrant(granted, reached)
    }

    /**
     * Dün ile geriye doğru kesintisiz kaçırılan günleri kapatır.
     * Zincirin öbür ucunda tekrar yapılmış veya dondurulmuş bir gün yoksa hiçbir şey tüketmez.
     * Çok günlük boşluk ancak tamamı stokla kapatılabiliyorsa tüketilir; aksi halde seri kırılır.
     * Bugün asla dondurulmaz ve aynı gün ikinci kez dondurulmaz.
     */
    fun decide(
        reviewedDays: Set<LocalDate>,
        frozenDays: Set<LocalDate>,
        today: LocalDate,
        stock: Int,
    ): FreezeDecision {
        val safeStock = stock.coerceIn(0, MAX_FREEZES)
        if (safeStock == 0) return FreezeDecision(emptyList(), 0)

        var cursor = today.minusDays(1)
        val missed = ArrayList<LocalDate>()
        var guard = 0
        while (guard++ < MAX_LOOKBACK) {
            if (cursor in reviewedDays || cursor in frozenDays) {
                if (missed.isEmpty()) return FreezeDecision(emptyList(), safeStock)
                return FreezeDecision(missed.asReversed(), safeStock - missed.size)
            }
            missed += cursor
            if (missed.size > safeStock) {
                return FreezeDecision(emptyList(), safeStock)
            }
            cursor = cursor.minusDays(1)
        }
        return FreezeDecision(emptyList(), safeStock)
    }
}
