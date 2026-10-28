package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.ReviewLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeeklyStats(
    val activeDays: Int,
    val cards: Int,
    val streak: Int,
)

/**
 * Haftalık özet bildirimi için saf yardımcılar: tetikleme zamanı, son 7 günün
 * özeti ve Türkçe metin.
 */
object WeeklySummary {

    const val HOUR = 18

    fun nextSundayAt(
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        hour: Int = HOUR,
    ): Long {
        val current = Instant.ofEpochMilli(now).atZone(zone)
        var target = current
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(hour.coerceIn(0, 23))
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(current)) {
            target = target.plusWeeks(1)
        }
        return target.toInstant().toEpochMilli()
    }

    fun stats(
        logs: List<ReviewLog>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        frozenDays: Set<LocalDate> = emptySet(),
    ): WeeklyStats {
        val byDay = StatsMath.reviewsByDay(logs, zone)
        val start = today.minusDays(6)
        var activeDays = 0
        var cards = 0
        for (offset in 0L..6L) {
            val count = byDay[start.plusDays(offset)] ?: 0
            if (count > 0) activeDays++
            cards += count
        }
        return WeeklyStats(
            activeDays = activeDays,
            cards = cards,
            streak = StatsMath.streak(byDay, today, frozenDays),
        )
    }

    fun summaryText(stats: WeeklyStats): String =
        summaryText(stats.activeDays, stats.cards, stats.streak)

    fun summaryText(activeDays: Int, cards: Int, streak: Int): String =
        "Bu hafta $activeDays gün çalıştın · $cards kart · $streak günlük seri"
}
